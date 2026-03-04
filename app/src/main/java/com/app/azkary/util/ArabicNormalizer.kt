package com.app.azkary.util

object ArabicNormalizer {
    /**
     * Normalizes Arabic text for stable length calculation:
     * - Removes diacritics (tashkeel/harakat)
     * - Removes whitespace and punctuation
     */
    fun normalize(text: String?): String {
        if (text == null) return ""
        return text
            // Strip diacritics: 064B–065F, 0670, 06D6–06ED
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
            // Remove whitespace (including non-breaking space \u00A0)
            .replace(Regex("[\\s\\u00A0]+"), "")
            // Remove punctuation and symbols
            .replace(Regex("[\\p{P}\\p{S}]"), "")
    }

}
