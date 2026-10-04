package com.neurasamu.build.samu_chat.smg

import com.neurasamu.build.samu_chat.network.ChatClient
import com.neurasamu.build.samu_chat.network.ChatEvent
import com.neurasamu.build.samu_chat.network.ChatRequest
import kotlinx.coroutines.flow.toList

/**
 * Extracts 1-20 atomic keywords from a user+assistant turn.
 * Uses the loaded LLM itself — no separate model needed.
 */
object KeywordExtractor {

    private const val SYSTEM = """
You extract keywords from conversations. Given a user message and the assistant's reply, output ONLY 3-7 short keywords separated by commas.

Rules:
- Lowercase
- Single words or short 2-word phrases only
- No punctuation except commas
- No explanations, no quotes, no bullet points
- Focus on: topic, entities, decisions, facts
- Skip stopwords (the, a, is, and, of, etc.)

Output format: keyword1, keyword2, keyword3
""".trimIndent()

    suspend fun extract(
        userText: String,
        assistantText: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ): String {
        val turn = "User: $userText\n\nAssistant: $assistantText"
        val req = ChatRequest(
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            messages = listOf(
                "system" to SYSTEM,
                "user" to turn
            ),
            temperature = 0.1,
            topP = 0.9,
            maxTokens = 60,
            stream = false
        )

        val sb = StringBuilder()
        return try {
            ChatClient.stream(req).toList().forEach { ev ->
                when (ev) {
                    is ChatEvent.Token -> sb.append(ev.text)
                    is ChatEvent.Done -> if (sb.isEmpty()) sb.append(ev.fullText)
                    is ChatEvent.Error -> return ""
                }
            }
            parseKeywords(sb.toString())
        } catch (_: Exception) {
            ""
        }
    }

    /** Fallback keyword extraction without LLM — simple heuristic. */
    fun extractHeuristic(text: String): String {
        val stop = setOf(
            "the","a","an","is","are","was","were","be","been","being",
            "have","has","had","do","does","did","will","would","could",
            "should","can","may","might","must","shall","to","of","in",
            "on","at","by","for","with","about","as","into","like","through",
            "and","or","but","if","then","else","when","where","why","how",
            "what","which","who","whom","this","that","these","those",
            "i","you","he","she","it","we","they","me","him","her","us",
            "them","my","your","his","her","its","our","their",
            "am","not","no","yes","ok","okay","hi","hello","hey","thanks"
        )
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in stop }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(7)
            .joinToString(", ") { it.key }
    }

    /** Clean LLM output into canonical "a, b, c" form. */
    private fun parseKeywords(raw: String): String {
        return raw
            .lines()
            .firstOrNull { it.isNotBlank() }
            ?.replace(Regex("(?i)^keywords?[:\\s]*"), "")
            ?.split(",", ";", "|")
            ?.map { it.trim().lowercase().trim('"', '\'', '.', '-') }
            ?.filter { it.isNotBlank() && it.length <= 30 }
            ?.distinct()
            ?.take(20)
            ?.joinToString(", ")
            ?: ""
    }
}
