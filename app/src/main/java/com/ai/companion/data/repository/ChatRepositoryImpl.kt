package com.ai.companion.data.repository

import com.ai.companion.data.local.AppPreferences
import com.ai.companion.data.local.dao.ChatMessageDao
import com.ai.companion.data.remote.api.DeepSeekApi
import com.ai.companion.data.remote.api.DoubaoApi
import com.ai.companion.data.remote.api.VolcengineApi
import com.ai.companion.domain.model.ApiConfig
import com.ai.companion.domain.model.CharacterConfig
import com.ai.companion.domain.model.ChatMessage
import com.ai.companion.domain.model.MemoryContext
import com.ai.companion.domain.model.SenderType
import com.ai.companion.domain.repository.ChatRepository
import com.ai.companion.domain.usecase.HumanizeService
import com.ai.companion.domain.usecase.MemoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val appPreferences: AppPreferences,
    private val memoryService: MemoryService,
    private val humanizeService: HumanizeService,
    private val deepSeekApi: DeepSeekApi
) : ChatRepository {

    private val MAX_HISTORY_MESSAGES = 20 // 最多保留20条历史消息

    override fun getMessagesBySession(sessionId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesBySession(sessionId)
    }

    override suspend fun sendMessage(
        content: String,
        sessionId: String,
        characterConfig: CharacterConfig,
        apiConfig: ApiConfig
    ): ChatMessage = withContext(Dispatchers.IO) {
        // 1. 保存用户消息
        val userMessage = ChatMessage(
            content = content,
            senderType = SenderType.USER,
            sessionId = sessionId
        )
        chatMessageDao.insertMessage(userMessage)

        // 2. 检查API配置
        if (!apiConfig.isDeepSeekConfigured()) {
            throw IllegalStateException("DeepSeek API Key 未配置，请在设置页面配置")
        }

        // 3. 获取相关记忆用于上下文增强
        val memoryContext = memoryService.getRelevantMemories(content)

        // 4. 构建消息上下文
        val historyMessages = chatMessageDao.getMessagesBySession(sessionId).first()
            .takeLast(MAX_HISTORY_MESSAGES)

        val apiMessages = buildApiMessages(
            userMessage = content,
            history = historyMessages,
            characterConfig = characterConfig,
            memoryContext = memoryContext
        )

        // 5. 创建临时AI消息（显示"正在输入"状态）
        val tempAiMessage = ChatMessage(
            content = "...",
            senderType = SenderType.AI,
            sessionId = sessionId
        )
        chatMessageDao.insertMessage(tempAiMessage)

        try {
            // 6. 调用DeepSeek API（流式）
            var finalContent = ""
            
            deepSeekApi.streamChatCompletion(
                apiKey = apiConfig.deepSeekApiKey,
                messages = apiMessages,
                temperature = 0.7f,
                maxTokens = 800
            ).collect { result ->
                result.onSuccess { currentContent ->
                    finalContent = currentContent
                    // 更新消息（流式显示）
                    val updatedMessage = tempAiMessage.copy(
                        content = currentContent,
                        timestamp = System.currentTimeMillis()
                    )
                    chatMessageDao.insertMessage(updatedMessage)
                }.onFailure { error ->
                    throw error
                }
            }

            // 7. 对最终回复进行人性化处理
            val humanizedContent = humanizeService.humanizeResponse(
                original = finalContent,
                characterConfig = characterConfig
            )

            // 8. 更新为最终处理后的消息
            val finalAiMessage = tempAiMessage.copy(
                content = humanizedContent,
                timestamp = System.currentTimeMillis()
            )
            chatMessageDao.insertMessage(finalAiMessage)

            // 9. 异步触发记忆提取和摘要生成
            withContext(Dispatchers.Default) {
                val allMessages = chatMessageDao.getMessagesBySession(sessionId).first()
                if (allMessages.size % 10 == 0) {
                    memoryService.extractMemoriesFromConversation(
                        messages = allMessages,
                        apiKey = apiConfig.deepSeekApiKey,
                        sessionId = sessionId
                    )

                    memoryService.generateConversationSummary(
                        messages = allMessages,
                        apiKey = apiConfig.deepSeekApiKey,
                        sessionId = sessionId
                    )
                }
            }

            return@withContext finalAiMessage

        } catch (e: Exception) {
            // 删除临时消息，返回错误信息
            chatMessageDao.deleteMessage(tempAiMessage)
            
            val errorMessage = ChatMessage(
                content = "抱歉，消息发送失败：${e.message ?: "未知错误"}",
                senderType = SenderType.AI,
                sessionId = sessionId
            )
            chatMessageDao.insertMessage(errorMessage)
            
            throw e
        }
    }

    /**
     * 构建API消息，包含系统提示、人设、记忆、历史消息
     */
    private fun buildApiMessages(
        userMessage: String,
        history: List<ChatMessage>,
        characterConfig: CharacterConfig,
        memoryContext: MemoryContext
    ): List<DeepSeekApi.ChatMessage> {
        val result = mutableListOf<DeepSeekApi.ChatMessage>()

        // 1. 系统提示（人设 + 记忆）
        val systemPrompt = buildSystemPrompt(characterConfig, memoryContext)
        if (systemPrompt.isNotEmpty()) {
            result.add(DeepSeekApi.ChatMessage("system", systemPrompt))
        }

        // 2. 历史消息
        history.forEach { msg ->
            val role = if (msg.senderType == SenderType.USER) "user" else "assistant"
            result.add(DeepSeekApi.ChatMessage(role, msg.content))
        }

        // 3. 当前用户消息
        result.add(DeepSeekApi.ChatMessage("user", userMessage))

        return result
    }

    /**
     * 构建系统提示
     */
    private fun buildSystemPrompt(
        characterConfig: CharacterConfig,
        memoryContext: MemoryContext
    ): String {
        return buildString {
            // 人设配置
            if (!characterConfig.isEmpty()) {
                append(characterConfig.toPrompt())
                append("\n\n")
            }

            // 记忆上下文
            val memoryPrompt = memoryContext.toPrompt()
            if (memoryPrompt.isNotEmpty()) {
                append(memoryPrompt)
                append("\n")
            }

            // 去AI化指令
            append(
                """
                ### 回复要求 ###
                1. 请用自然口语化的方式回复，避免过于正式或生硬
                2. 回复长度适中，不要太长太啰嗦
                3. 绝对不要提及你是AI、人工智能、语言模型等
                4. 回复要有温度，像真实朋友一样交流
                5. 可以适当使用语气词，让对话更生动
                """.trimIndent()
            )
        }
    }

    override suspend fun insertMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(message)
    }

    override suspend fun deleteSessionMessages(sessionId: String) {
        chatMessageDao.deleteSessionMessages(sessionId)
    }

    override suspend fun deleteAllMessages() {
        chatMessageDao.deleteAllMessages()
        memoryService.clearAllMemories()
    }

    override fun getCharacterConfig(): Flow<CharacterConfig> {
        return combine(
            appPreferences.characterName,
            appPreferences.characterPersonality,
            appPreferences.characterBackstory,
            appPreferences.characterSpeakingStyle,
            appPreferences.characterAvatar
        ) { name, personality, backstory, speakingStyle, avatar ->
            CharacterConfig(
                name = name ?: "",
                personality = personality ?: "",
                backstory = backstory ?: "",
                speakingStyle = speakingStyle ?: "",
                avatar = avatar ?: ""
            )
        }
    }

    override suspend fun saveCharacterConfig(config: CharacterConfig) {
        appPreferences.setCharacterName(config.name)
        appPreferences.setCharacterPersonality(config.personality)
        appPreferences.setCharacterBackstory(config.backstory)
        appPreferences.setCharacterSpeakingStyle(config.speakingStyle)
        appPreferences.setCharacterAvatar(config.avatar)
    }

    override fun getApiConfig(): Flow<ApiConfig> {
        return combine(
            appPreferences.deepSeekApiKey,
            appPreferences.volcengineAppId,
            appPreferences.volcengineAccessKey,
            appPreferences.volcengineSecretKey,
            appPreferences.doubaoImageKey
        ) { deepSeek, appId, accessKey, secretKey, doubao ->
            ApiConfig(
                deepSeekApiKey = deepSeek ?: "",
                volcengineAppId = appId ?: "",
                volcengineAccessKey = accessKey ?: "",
                volcengineSecretKey = secretKey ?: "",
                doubaoImageKey = doubao ?: ""
            )
        }
    }

    override suspend fun saveApiConfig(config: ApiConfig) {
        appPreferences.setDeepSeekApiKey(config.deepSeekApiKey)
        appPreferences.setVolcengineAppId(config.volcengineAppId)
        appPreferences.setVolcengineAccessKey(config.volcengineAccessKey)
        appPreferences.setVolcengineSecretKey(config.volcengineSecretKey)
        appPreferences.setDoubaoImageKey(config.doubaoImageKey)
    }

    override suspend fun testDeepSeekApi(apiKey: String): Boolean {
        return deepSeekApi.testConnection(apiKey)
    }

    override suspend fun testVolcengineApi(
        appId: String,
        accessKey: String,
        secretKey: String
    ): Boolean {
        return VolcengineApi.testConnection(accessKey, secretKey, appId)
    }

    override suspend fun testDoubaoApi(apiKey: String): Boolean {
        return DoubaoApi.testConnection(apiKey)
    }

    override fun getAllSessions(): Flow<List<String>> {
        return chatMessageDao.getAllSessions()
    }

    // 豆包图像生成
    override suspend fun generateImage(
        apiKey: String,
        prompt: String
    ): Result<List<DoubaoApi.ImageGenerationResult>> {
        return DoubaoApi.generateImage(apiKey, prompt)
    }

    override suspend fun downloadImage(url: String, outputFile: File): Result<File> {
        return DoubaoApi.downloadImage(url, outputFile)
    }

    // 火山引擎 ASR
    override suspend fun speechToText(
        accessKey: String,
        secretKey: String,
        appId: String,
        audioFile: File
    ): Result<String> {
        return VolcengineApi.speechToText(accessKey, secretKey, appId, audioFile)
    }

    // 火山引擎 TTS
    override suspend fun textToSpeech(
        accessKey: String,
        secretKey: String,
        appId: String,
        text: String
    ): Result<File> {
        return VolcengineApi.textToSpeech(accessKey, secretKey, appId, text)
    }

    // 火山引擎 OCR
    override suspend fun recognizeImage(
        accessKey: String,
        secretKey: String,
        appId: String,
        imageFile: File
    ): Result<String> {
        return VolcengineApi.recognizeImage(accessKey, secretKey, appId, imageFile)
    }

    // 记忆相关
    override fun getAllMemories() = memoryService.getAllMemories()

    override suspend fun clearAllMemories() {
        memoryService.clearAllMemories()
    }
}
