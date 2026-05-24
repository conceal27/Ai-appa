package com.ai.companion.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ai.companion.data.local.dao.ChatMessageDao
import com.ai.companion.data.local.dao.MemoryDao
import com.ai.companion.domain.model.ChatMessage
import com.ai.companion.domain.model.ConversationSummary
import com.ai.companion.domain.model.LongTermMemory

@Database(
    entities = [
        ChatMessage::class,
        LongTermMemory::class,
        ConversationSummary::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        const val DATABASE_NAME = "ai_companion_db"
    }
}
