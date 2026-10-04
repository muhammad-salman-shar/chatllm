package com.neurasamu.build.samu_chat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.neurasamu.build.samu_chat.smg.SmgBundle
import com.neurasamu.build.samu_chat.smg.SmgDao

@Database(
    entities = [ApiConfig::class, Conversation::class, Message::class, SmgBundle::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apiConfigDao(): ApiConfigDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun smgDao(): SmgDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(ctx: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDatabase::class.java,
                    "samu_chat.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
