package com.neurasamu.build.samu_chat.smg

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SMG Keyword Bundle — the "parchi" (slip).
 * Points to a full turn (user + assistant messages) stored in CS (Chat Store = messages table).
 */
@Entity(
    tableName = "keyword_bundles",
    indices = [
        Index("conversationId"),
        Index("createdAt")
    ]
)
data class SmgBundle(
    @PrimaryKey val id: String,          // "KC-1", "KC-2", ...
    val conversationId: String,
    val userMessageId: String,           // → messages.id
    val assistantMessageId: String,      // → messages.id
    val keywords: String,                // space-separated, 1-20 terms
    val createdAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0,
    val lastAccessedAt: Long = 0L
)
