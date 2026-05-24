package com.ai.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ai.companion.domain.model.ConversationSummary
import com.ai.companion.domain.model.LongTermMemory
import com.ai.companion.domain.model.MemoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    // LongTermMemory 操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: LongTermMemory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<LongTermMemory>)

    @Update
    suspend fun updateMemory(memory: LongTermMemory)

    @Delete
    suspend fun deleteMemory(memory: LongTermMemory)

    @Query("SELECT * FROM long_term_memories ORDER BY importance DESC, createdAt DESC")
    fun getAllMemories(): Flow<List<LongTermMemory>>

    @Query("SELECT * FROM long_term_memories WHERE type = :type ORDER BY importance DESC, createdAt DESC")
    fun getMemoriesByType(type: MemoryType): Flow<List<LongTermMemory>>

    @Query("SELECT * FROM long_term_memories ORDER BY lastAccessedAt DESC LIMIT :limit")
    fun getRecentMemories(limit: Int): Flow<List<LongTermMemory>>

    @Query("SELECT * FROM long_term_memories WHERE content LIKE '%' || :query || '%' ORDER BY importance DESC")
    suspend fun searchMemories(query: String): List<LongTermMemory>

    @Query("UPDATE long_term_memories SET accessCount = accessCount + 1, lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun incrementAccessCount(id: Long, timestamp: Long)

    @Query("DELETE FROM long_term_memories")
    suspend fun clearAllMemories()

    // ConversationSummary 操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: ConversationSummary)

    @Query("SELECT * FROM conversation_summaries WHERE sessionId = :sessionId")
    suspend fun getSummaryBySession(sessionId: String): ConversationSummary?

    @Query("SELECT * FROM conversation_summaries ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentSummaries(limit: Int): Flow<List<ConversationSummary>>

    @Query("DELETE FROM conversation_summaries WHERE sessionId = :sessionId")
    suspend fun deleteSummary(sessionId: String)

    @Query("DELETE FROM conversation_summaries")
    suspend fun clearAllSummaries()
}
