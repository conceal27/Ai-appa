package com.ai.companion.domain.repository

import com.ai.companion.data.remote.api.DoubaoApi
import com.ai.companion.domain.model.ApiConfig
import com.ai.companion.domain.model.CharacterConfig
import com.ai.companion.domain.model.ChatMessage
import com.ai.companion.domain.model.LongTermMemory
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ChatRepository {
    // 聊天消息
    fun getMessagesBySession(sessionId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(
        content: String,
        sessionId: String,
        characterConfig: CharacterConfig,
        apiConfig: ApiConfig
    ): ChatMessage

    suspend fun insertMessage(message: ChatMessage)
    suspend fun deleteSessionMessages(sessionId: String)
    suspend fun deleteAllMessages()

    // 人设配置
    fun getCharacterConfig(): Flow<CharacterConfig>
    suspend fun saveCharacterConfig(config: CharacterConfig)

    // API配置
    fun getApiConfig(): Flow<ApiConfig>
    suspend fun saveApiConfig(config: ApiConfig)
    suspend fun testDeepSeekApi(apiKey: String): Boolean
    suspend fun testVolcengineApi(appId: String, accessKey: String, secretKey: String): Boolean
    suspend fun testDoubaoApi(apiKey: String): Boolean

    // 会话管理
    fun getAllSessions(): Flow<List<String>>

    // 豆包图像生成
    suspend fun generateImage(
        apiKey: String,
        prompt: String
    ): Result<List<DoubaoApi.ImageGenerationResult>>

    suspend fun downloadImage(url: String, outputFile: File): Result<File>

    // 火山引擎 ASR
    suspend fun speechToText(
        accessKey: String,
        secretKey: String,
        appId: String,
        audioFile: File
    ): Result<String>

    // 火山引擎 TTS
    suspend fun textToSpeech(
        accessKey: String,
        secretKey: String,
        appId: String,
        text: String
    ): Result<File>

    // 火山引擎 OCR
    suspend fun recognizeImage(
        accessKey: String,
        secretKey: String,
        appId: String,
        imageFile: File
    ): Result<String>

    // 记忆管理
    fun getAllMemories(): Flow<List<LongTermMemory>>
    suspend fun clearAllMemories()
}
