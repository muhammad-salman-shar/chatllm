package com.neurasamu.build.samu_chat.smg

import android.content.Context
import com.neurasamu.build.samu_chat.data.ApiConfig

/**
 * Given a user query, find the top-K most relevant bundles for a conversation.
 * Strategy: extract query keywords (LLM preferred, heuristic fallback), then
 * match against stored bundle keywords using substring matching.
 */
class SmgRetriever(ctx: Context) {

    private val kc = KcController(ctx)

    data class Hit(
        val bundle: SmgBundle,
        val score: Int
    )

    suspend fun retrieve(
        convId: String,
        userQuery: String,
        api: ApiConfig,
        topK: Int = 2
    ): List<Hit> {
        val bundles = kc.listBundles(convId)
        if (bundles.isEmpty()) return emptyList()

        // Try LLM keyword extraction first, fallback to heuristic
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

        val terms = queryKeywords.split(",").map { it.trim().lowercase() }
            .filter { it.isNotBlank() && it.length >= 3 }

        if (terms.isEmpty()) return emptyList()

        // Score each bundle: how many query terms match its keywords
        val scored = bundles.mapNotNull { bundle ->
            val bundleWords = bundle.keywords.lowercase().split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val score = terms.count { term ->
                bundleWords.any { bw -> bw.contains(term) || term.contains(bw) }
            }
            if (score > 0) Hit(bundle, score) else null
        }

        return scored.sortedByDescending { it.score }.take(topK)
    }
}
