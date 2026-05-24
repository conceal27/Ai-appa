package com.ai.companion.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * 记忆类型枚举
 */
enum class MemoryType {
    USER_PREFERENCE,    // 用户喜好
    IMPORTANT_EVENT,    // 重要事件
    PERSONAL_INFO,      // 个人信息
    CONVERSATION_SUMMARY, // 对话摘要
    CUSTOM              // 自定义
}

/**
 * 长期记忆实体
 */
@Entity(tableName = "long_term_memories")
data class LongTermMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,           // 记忆内容
    val type: MemoryType,          // 记忆类型
    val sourceMessageId: Long? = null, // 来源消息ID
    val importance: Int = 5,       // 重要程度 1-10
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0
)

/**
 * 对话摘要实体
 */
@Entity(tableName = "conversation_summaries")
data class ConversationSummary(
    @PrimaryKey val sessionId: String,
    val summary: String,           // 摘要内容
    val keyPoints: String = "",    // 要点列表（JSON格式）
    val messageCount: Int = 0,     // 摘要涵盖的消息数量
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 记忆关联结果
 */
data class MemoryContext(
    val relevantMemories: List<LongTermMemory>,
    val summary: String? = null,
    val systemPromptAddition: String = ""
) {
    fun toPrompt(): String {
        return buildString {
            if (relevantMemories.isNotEmpty()) {
                append("### 关于用户的重要信息 ###\n")
                relevantMemories.forEach { memory ->
                    append("- ${memory.content}\n")
                }
                append("\n")
            }
            
            if (!summary.isNullOrEmpty()) {
                append("### 之前对话摘要 ###\n")
                append("$summary\n\n")
            }
            
            if (systemPromptAddition.isNotEmpty()) {
                append("### 对话风格提示 ###\n")
                append("$systemPromptAddition\n")
            }
        }
    }
}
