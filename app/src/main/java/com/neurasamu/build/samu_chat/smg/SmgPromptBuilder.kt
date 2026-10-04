package com.neurasamu.build.samu_chat.smg

import android.content.Context

/**
 * Builds a bounded prompt: keyword slip list + relevant recalled turns + recent context.
 * Keeps the LLM's active context small — full history stays in Chat Store.
 */
class SmgPromptBuilder(ctx: Context) {

    private val kc = KcController(ctx)

    /**
     * Turn bundles into a compact "memory index" block for the system prompt.
     * Example:
     *   [Memory index — 6 slips available]
     *   KC-1: gym, kandha, dard
     *   KC-2: protein, diet
     */
    suspend fun buildMemoryIndex(convId: String): String {
        val bundles = kc.listBundles(convId)
        if (bundles.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("[Memory index — ").append(bundles.size).append(" slips]\n")
        bundles.takeLast(20).forEach { b ->
            sb.append(b.id).append(": ").append(b.keywords).append("\n")
        }
        return sb.toString().trimEnd()
    }

    /**
     * Turn top-K retrieved bundles into a bounded recalled-context block.
     * Only includes the raw turn payloads that were actually matched.
     */
    suspend fun buildRecalledContext(hits: List<SmgRetriever.Hit>): String {
        if (hits.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("[Recalled memory — ").append(hits.size).append(" slip(s)]\n")
        hits.forEach { hit ->
            val payload = kc.fetchPayload(hit.bundle)
            if (payload != null) {
                val (user, assistant) = payload
                sb.append("• ").append(hit.bundle.id).append(" (").append(hit.bundle.keywords).append(")\n")
                sb.append("  User: ").append(user.content.take(500)).append("\n")
                sb.append("  Assistant: ").append(assistant.content.take(500)).append("\n\n")
            }
        }
        return sb.toString().trimEnd()
    }
}
