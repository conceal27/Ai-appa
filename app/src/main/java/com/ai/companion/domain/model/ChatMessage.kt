package com.ai.companion.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val senderType: SenderType,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String,
    val isRead: Boolean = true
)

enum class SenderType {
    USER,
    AI
}
