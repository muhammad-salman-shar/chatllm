package com.neurasamu.build.samu_chat.smg

import android.content.Context

/**
 * Builds a bounded prompt:
 *   1) full slip index (so model knows what's available)
 *   2) recalled turns for the current query (auto-retrieved)
 *   3) recent context window
 */
class SmgPromptBuilder(ctx: Context) {

    private val kc = KcController(ctx)
    private val retriever = SmgRetriever(ctx)

    /** Full compact slip index — always included when SMG is on. */
    suspend fun buildMemoryIndex(convId: String): String =
        retriever.listAllSlipsCompact(convId)

    /** Recalled turns for the current query — bounded, top-K only. */
    suspend fun buildRecalledContext(hits: List<SmgRetriever.Hit>): String {
        if (hits.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("[Recalled memory — ").append(hits.size).append(" slip(s)]\n")
        hits.forEach { hit ->
            val payload = kc.fetchPayload(hit.bundle)
            if (payload != null) {
                val (user, assistant) = payload
                sb.append("• ").append(hit.bundle.id)
                    .append(" (").append(hit.bundle.keywords).append(")\n")
                sb.append("  User: ").append(user.content.take(400)).append("\n")
                sb.append("  Assistant: ").append(assistant.content.take(400)).append("\n\n")
            }
        }
        return sb.toString().trimEnd()
    }
}
