package com.app.azkary.util

object ArabicNormalizer {
    private val diacriticsRegex = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val whitespaceRegex = Regex("[\\s\\u00A0]+")
    private val punctuationRegex = Regex("[\\p{P}\\p{S}]")

    /**
     * Normalizes Arabic text for stable length calculation:
     * - Removes diacritics (tashkeel/harakat)
     * - Removes whitespace and punctuation
     */
    fun normalize(text: String?): String {
        if (text == null) return ""
        return text
            .replace(diacriticsRegex, "")
            .replace(whitespaceRegex, "")
            .replace(punctuationRegex, "")
    }

    /**
     * Normalizes text for search purposes:
     * - Removes diacritics (tashkeel/harakat)
     * - Normalizes whitespace to single spaces
     * - Converts to lowercase
     */
    fun normalizeForSearch(text: String?): String {
        if (text == null) return ""
        return text
            .replace(diacriticsRegex, "")
            .replace(whitespaceRegex, " ")
            .trim()
            .lowercase()
    }

    /**
     * Performs fuzzy matching combining normalized contains and Levenshtein similarity.
     * @param query The search query
     * @param text The text to search in (nullable)
     * @param threshold Minimum similarity ratio (0.0 to 1.0) for Levenshtein match
     * @return true if the text matches the query
     */
    fun fuzzyMatch(query: String, text: String?, threshold: Float = 0.7f): Boolean {
        if (text.isNullOrBlank()) return false
        if (query.isBlank()) return true

        val normalizedQuery = normalizeForSearch(query)
        val normalizedText = normalizeForSearch(text)

        if (normalizedQuery.isEmpty() || normalizedText.isEmpty()) return false

        if (normalizedText.contains(normalizedQuery)) return true

        return levenshteinSimilarity(normalizedQuery, normalizedText) >= threshold
    }

    /**
     * Calculates Levenshtein similarity ratio between two strings.
     * Returns a value between 0.0 (completely different) and 1.0 (identical).
     */
    private fun levenshteinSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val distance = levenshteinDistance(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)
        return 1.0f - (distance.toFloat() / maxLength)
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[len1][len2]
    }
}
