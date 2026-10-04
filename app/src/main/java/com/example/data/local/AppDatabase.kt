package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.McpServerDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.SkillDao
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.McpServerEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SkillEntity

@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        ProjectEntity::class,
        SkillEntity::class,
        McpServerEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun projectDao(): ProjectDao
    abstract fun skillDao(): SkillDao
    abstract fun mcpServerDao(): McpServerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "router_agent_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
