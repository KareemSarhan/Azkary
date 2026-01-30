package com.app.azkary.util

import android.content.Context
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import java.util.Locale

/**
 * Helper utility for handling bidirectional text (BiDi) in mixed content scenarios.
 *
 * This class uses AndroidX Core's BidiFormatter to properly handle strings that mix
 * Arabic text with numbers and Latin characters, ensuring correct display in both
 * RTL (Arabic) and LTR (English) contexts.
 *
 * Common use cases:
 * - Page counters like "(1/10)"
 * - Progress percentages like "75%"
 * - Verse references like "[Bukhari 123]"
 * - Any mixed Arabic + numbers + Latin text
 */
object BidiHelper {

    /**
     * Gets the BidiFormatter instance for the given context's locale.
     *
     * @param context The Android context
     * @return A BidiFormatter instance configured for the current locale
     */
    private fun getBidiFormatter(context: Context): BidiFormatter {
        val locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()
        return BidiFormatter.getInstance(locale)
    }

    /**
     * Formats a page counter with proper bidirectional handling.
     *
     * Example outputs:
     * - In LTR: "(1/10)"
     * - In RTL: "(1/10)" with proper number ordering
     *
     * @param current The current page number
     * @param total The total number of pages
     * @param context The Android context
     * @return A formatted page counter string with proper bidi handling
     */
    fun formatPageCounter(current: Int, total: Int, context: Context): String {
        val formatter = getBidiFormatter(context)
        val pageText = "($current/$total)"
        return formatter.unicodeWrap(pageText, TextDirectionHeuristicsCompat.LTR)
    }

    /**
     * Formats a progress percentage with proper bidirectional handling.
     *
     * Example outputs:
     * - In LTR: "75%"
     * - In RTL: "75%" with proper number ordering
     *
     * @param percent The progress percentage (0-100)
     * @param context The Android context
     * @return A formatted percentage string with proper bidi handling
     */

    fun formatProgress(percent: Int, context: Context): String {
        val formatter = getBidiFormatter(context) // returns androidx.core.text.BidiFormatter
        val progressText = "$percent%"
        return formatter.unicodeWrap(progressText, TextDirectionHeuristicsCompat.LTR)
    }



    /**
     * Formats a repeat counter with proper bidirectional handling.
     *
     * Example outputs:
     * - In LTR: "5/33"
     * - In RTL: "5/33" with proper number ordering
     *
     * @param current The current count
     * @param required The required count
     * @param context The Android context
     * @return A formatted repeat counter string with proper bidi handling
     */
    fun formatRepeatCounter(current: Int, required: Int, context: Context): String {
        val formatter = getBidiFormatter(context)
        val repeatText = "$current/$required"
        return formatter.unicodeWrap(repeatText, TextDirectionHeuristicsCompat.LTR)
    }

    /**
     * Formats a verse reference with proper bidirectional handling.
     *
     * Example outputs:
     * - In LTR: "[Bukhari 5074]"
     * - In RTL: "[Bukhari 5074]" with proper text direction
     *
     * @param reference The verse reference string (e.g., "[Bukhari 5074]")
     * @param context The Android context
     * @return A formatted reference string with proper bidi handling
     */
    fun formatVerseReference(reference: String, context: Context): String {
        val formatter = getBidiFormatter(context)
        return formatter.unicodeWrap(reference, TextDirectionHeuristicsCompat.LTR)
    }

    /**
     * Formats mixed content (Arabic + numbers + Latin) with proper bidirectional handling.
     *
     * This is a general-purpose formatter for any mixed content that needs
     * proper bidi handling. It automatically detects the direction based on the locale.
     *
     * @param text The mixed content text
     * @param context The Android context
     * @return The formatted text with proper bidi handling
     */
    fun formatMixedText(text: String, context: Context): String {
        val formatter = getBidiFormatter(context)
        return formatter.unicodeWrap(text)
    }

    /**
     * Formats text with explicit LTR direction.
     *
     * Use this when you want to force LTR direction regardless of the locale,
     * such as for numbers, page counters, or technical terms.
     *
     * @param text The text to format
     * @param context The Android context
     * @return The formatted text with LTR direction
     */
    fun formatAsLtr(text: String, context: Context): String {
        val formatter = getBidiFormatter(context)
        return formatter.unicodeWrap(text, TextDirectionHeuristicsCompat.LTR)
    }

    /**
     * Formats text with explicit RTL direction.
     *
     * Use this when you want to force RTL direction regardless of the locale.
     *
     * @param text The text to format
     * @param context The Android context
     * @return The formatted text with RTL direction
     */
    fun formatAsRtl(text: String, context: Context): String {
        val formatter = getBidiFormatter(context)
        return formatter.unicodeWrap(text, TextDirectionHeuristicsCompat.RTL)
    }
}
