package com.mira.memory.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mira.memory.dao.CharacterDao
import com.mira.memory.dao.ChatMessageDao
import com.mira.memory.dao.LocalModelDao
import com.mira.memory.dao.MemoryFactDao
import com.mira.memory.dao.StudioImageDao
import com.mira.memory.model.CharacterProfileEntity
import com.mira.memory.model.ChatMessageEntity
import com.mira.memory.model.LocalModelEntity
import com.mira.memory.model.MemoryFactEntity
import com.mira.memory.model.StudioImageEntity

@Database(
    entities = [
        CharacterProfileEntity::class,
        ChatMessageEntity::class,
        MemoryFactEntity::class,
        StudioImageEntity::class,
        LocalModelEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MiraDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun memoryFactDao(): MemoryFactDao
    abstract fun studioImageDao(): StudioImageDao
    abstract fun localModelDao(): LocalModelDao

    companion object {
        @Volatile
        private var INSTANCE: MiraDatabase? = null

        fun getInstance(context: Context): MiraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MiraDatabase::class.java,
                    "mira_local_vault.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
