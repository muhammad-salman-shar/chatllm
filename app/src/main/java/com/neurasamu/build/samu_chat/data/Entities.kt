package com.neurasamu.build.samu_chat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "api_configs")
data class ApiConfig(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    @ColumnInfo(defaultValue = "")
    val systemPrompt: String = "",
    @ColumnInfo(defaultValue = "4096")
    val contextWindow: Int = 4096,
    @ColumnInfo(defaultValue = "1024")
    val maxTokensPerReply: Int = 1024,
    @ColumnInfo(defaultValue = "0.7")
    val temperature: Float = 0.7f,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val apiConfigId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
