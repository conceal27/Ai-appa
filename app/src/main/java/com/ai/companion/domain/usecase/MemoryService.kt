package com.ai.companion.domain.usecase

import com.ai.companion.data.local.dao.MemoryDao
import com.ai.companion.data.remote.api.DeepSeekApi
import com.ai.companion.domain.model.ChatMessage
import com.ai.companion.domain.model.ConversationSummary
import com.ai.companion.domain.model.LongTermMemory
import com.ai.companion.domain.model.MemoryContext
import com.ai.companion.domain.model.MemoryType
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryService @Inject constructor(
    private val memoryDao: MemoryDao
) {
    companion object {
        private const val SUMMARY_THRESHOLD = 10 // 每10条消息生成一次摘要
        private const val MAX_CONTEXT_MEMORIES = 5 // 最多注入5条相关记忆
    }

    /**
     * 从对话中提取关键信息并生成长期记忆
     */
    suspend fun extractMemoriesFromConversation(
        messages: List<ChatMessage>,
        apiKey: String,
        sessionId: String
    ): Result<List<LongTermMemory>> {
        if (messages.size < SUMMARY_THRESHOLD) return Result.success(emptyList())

        return try {
            val recentMessages = messages.takeLast(20)
            val prompt = buildMemoryExtractionPrompt(recentMessages)
            
            val result = DeepSeekApi.chatCompletion(
                apiKey = apiKey,
                messages = listOf(DeepSeekApi.ChatMessage("user", prompt)),
                temperature = 0.3f,
                maxTokens = 1024
            )

            if (result.isSuccess) {
                val response = result.getOrThrow()
                val extractedMemories = parseMemoryExtractionResponse(
                    responseText = response.choices.firstOrNull()?.message?.content ?: "",
                    sessionId = sessionId
                )
                
                extractedMemories.forEach { memoryDao.insertMemory(it) }
                Result.success(extractedMemories)
            } else {
                Result.failure(Exception("Failed to extract memories"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 生成对话摘要
     */
    suspend fun generateConversationSummary(
        messages: List<ChatMessage>,
        apiKey: String,
        sessionId: String
    ): Result<ConversationSummary> {
        if (messages.isEmpty()) return Result.failure(Exception("No messages to summarize"))

        return try {
            val prompt = buildSummaryPrompt(messages)
            
            val result = DeepSeekApi.chatCompletion(
                apiKey = apiKey,
                messages = listOf(DeepSeekApi.ChatMessage("user", prompt)),
                temperature = 0.3f,
                maxTokens = 512
            )

            if (result.isSuccess) {
                val response = result.getOrThrow()
                val content = response.choices.firstOrNull()?.message?.content ?: ""
                
                val summary = parseSummaryResponse(content, sessionId, messages.size)
                memoryDao.insertSummary(summary)
                Result.success(summary)
            } else {
                Result.failure(Exception("Failed to generate summary"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取相关记忆用于上下文注入
     */
    suspend fun getRelevantMemories(currentMessage: String): MemoryContext {
        val allMemories = memoryDao.getAllMemories().first()
        
        // 简单的关键词匹配，可以后续可替换为向量搜索
        val keywords = extractKeywords(currentMessage)
        val relevant = allMemories.filter { memory ->
            keywords.any { keyword ->
                memory.content.contains(keyword, ignoreCase = true)
            }
        }.take(MAX_CONTEXT_MEMORIES)

        // 更新访问时间
        relevant.forEach { memory ->
            memoryDao.incrementAccessCount(memory.id, System.currentTimeMillis())
        }

        return MemoryContext(
            relevantMemories = relevant,
            systemPromptAddition = ""
        )
    }

    /**
     * 构建记忆提取Prompt
     */
    private fun buildMemoryExtractionPrompt(messages: List<ChatMessage>): String {
        val conversationText = messages.joinToString("\n") { msg ->
            val sender = if (msg.senderType == com.ai.companion.domain.model.SenderType.USER) "用户" else "助手"
            "$sender: ${msg.content}"
        }

        return """
请分析以下对话，从中提取关于用户的重要信息并转化为结构化记忆。

对话内容：
$conversationText

请提取以下类型的信息：
1. USER_PREFERENCE: 用户的喜好、偏好、习惯
2. IMPORTANT_EVENT: 重要事件、重要日期、重要决定
3. PERSONAL_INFO: 用户的个人信息（年龄、职业、所在地等）

请以JSON数组格式返回，格式如下：
[
  {"type": "USER_PREFERENCE", "content": "具体内容", "importance": 1-10},
  {"type": "IMPORTANT_EVENT", "content": "具体内容", "importance": 1-10}
]

只返回JSON，不要其他文字说明。
        """.trimIndent()
    }

    /**
     * 构建摘要Prompt
     */
    private fun buildSummaryPrompt(messages: List<ChatMessage>): String {
        val conversationText = messages.joinToString("\n") { msg ->
            val sender = if (msg.senderType == com.ai.companion.domain.model.SenderType.USER) "用户" else "助手"
            "$sender: ${msg.content}"
        }

        return """
请总结以下对话的核心内容。

对话内容：
$conversationText

请返回JSON格式：
{
  "summary": "对话摘要，100字以内",
  "keyPoints": ["要点1", "要点2", "要点3"]
}

只返回JSON，不要其他文字说明。
        """.trimIndent()
    }

    /**
     * 解析记忆提取响应
     */
    private fun parseMemoryExtractionResponse(responseText: String, sessionId: String): List<LongTermMemory> {
        return try {
            val jsonStr = responseText.trim()
                .removeSurrounding("```json", "```")
                .removeSurrounding("```", "```")
                .trim()

            val jsonArray = JSONArray(jsonStr)
            val memories = mutableListOf<LongTermMemory>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val typeStr = item.optString("type", "CUSTOM")
                val type = try {
                    MemoryType.valueOf(typeStr)
                } catch (e: Exception) {
                    MemoryType.CUSTOM
                }

                memories.add(
                    LongTermMemory(
                        content = item.optString("content", ""),
                        type = type,
                        importance = item.optInt("importance", 5)
                    )
                )
            }

            memories
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 解析摘要响应
     */
    private fun parseSummaryResponse(
        responseText: String,
        sessionId: String,
        messageCount: Int
    ): ConversationSummary {
        return try {
            val jsonStr = responseText.trim()
                .removeSurrounding("```json", "```")
                .removeSurrounding("```", "```")
                .trim()

            val json = JSONObject(jsonStr)

            ConversationSummary(
                sessionId = sessionId,
                summary = json.optString("summary", ""),
                keyPoints = json.optJSONArray("keyPoints")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }.joinToString("|")
                } ?: "",
                messageCount = messageCount
            )
        } catch (e: Exception) {
            ConversationSummary(
                sessionId = sessionId,
                summary = responseText.take(200),
                messageCount = messageCount
            )
        }
    }

    /**
     * 简单关键词提取
     */
    private fun extractKeywords(text: String): List<String> {
        // 简单实现：按空格分割，过滤停用词
        val stopWords = setOf("的", "是", "在", "我", "你", "他", "她", "吗", "呢", "啊", "吧")
        return text.split(Regex("\\s+"))
            .filter { it.length >= 2 }
            .filter { it !in stopWords }
            .distinct()
    }

    /**
     * 获取所有记忆
     */
    fun getAllMemories() = memoryDao.getAllMemories()

    /**
     * 删除记忆
     */
    suspend fun deleteMemory(memory: LongTermMemory) {
        memoryDao.deleteMemory(memory)
    }

    /**
     * 清空所有记忆
     */
    suspend fun clearAllMemories() {
        memoryDao.clearAllMemories()
        memoryDao.clearAllSummaries()
    }
}
