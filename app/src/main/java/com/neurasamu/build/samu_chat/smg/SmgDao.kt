package com.neurasamu.build.samu_chat.smg

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SmgDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bundle: SmgBundle)

    @Query("SELECT * FROM keyword_bundles WHERE conversationId = :convId ORDER BY createdAt ASC")
    fun observeByConversation(convId: String): Flow<List<SmgBundle>>

    @Query("SELECT * FROM keyword_bundles WHERE conversationId = :convId ORDER BY createdAt ASC")
    suspend fun listByConversation(convId: String): List<SmgBundle>

    @Query("SELECT * FROM keyword_bundles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SmgBundle?

    /** Simple substring match — used as fallback retrieval. */
    @Query("SELECT * FROM keyword_bundles WHERE conversationId = :convId AND keywords LIKE '%' || :term || '%' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun searchByKeyword(convId: String, term: String, limit: Int): List<SmgBundle>

    @Query("UPDATE keyword_bundles SET accessCount = accessCount + 1, lastAccessedAt = :ts WHERE id = :id")
    suspend fun markAccessed(id: String, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM keyword_bundles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM keyword_bundles WHERE conversationId = :convId")
    suspend fun deleteByConversation(convId: String)

    @Query("SELECT COUNT(*) FROM keyword_bundles WHERE conversationId = :convId")
    suspend fun countForConversation(convId: String): Int
}
