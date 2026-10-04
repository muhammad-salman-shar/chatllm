package com.neurasamu.build.samu_chat.smg

import android.content.Context
import com.neurasamu.build.samu_chat.data.ApiConfig

/**
 * Given a user query, find the top-K most relevant bundles for a conversation.
 * Uses fuzzy word-by-word matching against stored bundle keywords.
 */
class SmgRetriever(ctx: Context) {

    private val kc = KcController(ctx)

    data class Hit(
        val bundle: SmgBundle,
        val score: Int
    )

    companion object {
        /** Words that make useless slip keywords. */
        val JUNK_WORDS = setOf(
            "assist", "help", "chat", "identity", "today", "hello", "hi", "hey",
            "okay", "ok", "yes", "no", "please", "thanks", "thank", "user",
            "assistant", "ai", "question", "answer", "reply", "response"
        )

        fun cleanKeywords(raw: String): List<String> {
            return raw.lowercase()
                .split(",", " ", ";", "|")
                .map { it.trim().trim('"', '\'', '.', '-', ':') }
                .filter { it.length >= 3 && it !in JUNK_WORDS }
                .distinct()
        }
    }

    suspend fun retrieve(
        convId: String,
        userQuery: String,
        api: ApiConfig,
        topK: Int = 3
    ): List<Hit> {
        val bundles = kc.listBundles(convId)
        if (bundles.isEmpty()) return emptyList()

        val queryKeywords = try {
            val llmResult = KeywordExtractor.extract(
                userText = userQuery,
                assistantText = "",
                baseUrl = api.baseUrl,
                apiKey = api.apiKey,
                model = api.modelName
            )
            if (llmResult.isBlank()) KeywordExtractor.extractHeuristic(userQuery)
            else llmResult
        } catch (_: Exception) {
            KeywordExtractor.extractHeuristic(userQuery)
        }

        if (queryKeywords.isBlank()) return emptyList()

        val queryTerms = cleanKeywords(queryKeywords)
        if (queryTerms.isEmpty()) return emptyList()

        android.util.Log.i("SmgRetriever",
            "Query terms: $queryTerms, bundles: ${bundles.size}")

        val scored = bundles.mapNotNull { bundle ->
            val bundleTerms = cleanKeywords(bundle.keywords)
            if (bundleTerms.isEmpty()) return@mapNotNull null

            var score = 0
            for (qt in queryTerms) {
                for (bt in bundleTerms) {
                    when {
                        qt == bt -> score += 3                       // exact word
                        qt.length >= 4 && bt.length >= 4 &&
                            (qt.contains(bt) || bt.contains(qt)) -> score += 2  // substring
                        // first-3-char prefix match (handles plural/tense)
                        qt.length >= 4 && bt.length >= 4 &&
                            qt.take(3) == bt.take(3) -> score += 1
                    }
                }
            }
            if (score > 0) Hit(bundle, score) else null
        }

        val result = scored.sortedByDescending { it.score }.take(topK)
        android.util.Log.i("SmgRetriever",
            "Hits: ${result.map { "${it.bundle.id}(${it.score})" }}")
        return result
    }

    /**
     * Compact list of ALL slips in this conversation. Always injected into
     * system prompt so the model can see what's available even if no
     * auto-retrieval hit happened.
     */
    suspend fun listAllSlipsCompact(convId: String): String {
        val bundles = kc.listBundles(convId)
        if (bundles.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("[Slips available: ").append(bundles.size).append("]\n")
        bundles.takeLast(30).forEach { b ->
            val clean = cleanKeywords(b.keywords).joinToString(", ")
            if (clean.isNotBlank()) {
                sb.append(b.id).append(": ").append(clean).append("\n")
            }
        }
        return sb.toString().trimEnd()
    }
}
