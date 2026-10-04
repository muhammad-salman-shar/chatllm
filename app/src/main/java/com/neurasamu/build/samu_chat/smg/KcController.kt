package com.neurasamu.build.samu_chat.smg

import android.content.Context
import com.neurasamu.build.samu_chat.data.ChatRepository
import com.neurasamu.build.samu_chat.data.Message

/**
 * KC = Keyword Controller.
 * The "daakiya" (postman) — assigns stable IDs (KC-1, KC-2…) and
 * fetches full turn payloads from Chat Store (CS) on demand.
 */
class KcController(ctx: Context) {

    private val repo = ChatRepository(ctx)
    private val dao = repo.dbSmg()

    /** Assign a new KC id for a conversation. */
    suspend fun nextId(convId: String): String {
        val count = dao.countForConversation(convId)
        return "KC-${count + 1}"
    }

    /** Create and persist a bundle for a completed turn. */
    suspend fun createBundle(
        convId: String,
        userMessageId: String,
        assistantMessageId: String,
        keywords: String
    ): SmgBundle? {
        if (keywords.isBlank()) return null
        val id = nextId(convId)
        val bundle = SmgBundle(
            id = id,
            conversationId = convId,
            userMessageId = userMessageId,
            assistantMessageId = assistantMessageId,
            keywords = keywords
        )
        dao.insert(bundle)
        return bundle
    }

    /** Fetch the raw turn payload (user + assistant messages) for a bundle. */
    suspend fun fetchPayload(bundle: SmgBundle): Pair<Message, Message>? {
        val user = repo.getMessagesByIds(listOf(bundle.userMessageId)).firstOrNull()
        val assistant = repo.getMessagesByIds(listOf(bundle.assistantMessageId)).firstOrNull()
        if (user == null || assistant == null) return null
        dao.markAccessed(bundle.id)
        return user to assistant
    }

    suspend fun listBundles(convId: String): List<SmgBundle> = dao.listByConversation(convId)

    suspend fun observeBundles(convId: String) = dao.observeByConversation(convId)

    suspend fun deleteBundle(id: String) = dao.deleteById(id)

    suspend fun deleteForConversation(convId: String) = dao.deleteByConversation(convId)
}
