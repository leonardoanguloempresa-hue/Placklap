package com.robopal.app.data.memory

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MemoryItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}
