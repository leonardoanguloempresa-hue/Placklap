package com.robopal.app.data.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_items")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general",
    val timestamp: Long = System.currentTimeMillis()
)
