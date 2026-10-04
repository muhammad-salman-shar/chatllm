package com.neurasamu.build.samu_chat.vm

/**
 * Rough token estimation: ~4 chars per token for English, less for other languages.
 * Used for UI display only — server does real tokenization.
 */
object TokenEstimator {
    fun estimate(text: String): Int {
        if (text.isBlank()) return 0
        // Conservative: 3.5 chars per token average (mixed language content)
        return (text.length / 3.5).toInt().coerceAtLeast(1)
    }

    fun estimateAll(messages: List<String>): Int =
        messages.sumOf { estimate(it) }
}
