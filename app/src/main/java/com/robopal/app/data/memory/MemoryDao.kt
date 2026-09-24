package com.robopal.app.data.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY timestamp DESC")
    suspend fun getAllMemories(): List<MemoryItem>

    @Query("SELECT * FROM memory_items WHERE category = :category ORDER BY timestamp DESC")
    suspend fun getMemoriesByCategory(category: String): List<MemoryItem>

    @Query("SELECT * FROM memory_items WHERE value LIKE '%' || :query || '%' OR key LIKE '%' || :query || '%'")
    suspend fun searchMemories(query: String): List<MemoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryItem)

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM memory_items")
    suspend fun clearAllMemories()
}
