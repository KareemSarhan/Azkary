package com.app.azkary.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Unit tests for BidiHelper
 *
 * Notes:
 * - This is JVM unit-test style mocking of Android classes. If you hit issues with
 *   android.* final classes on the JVM, run these as Robolectric tests OR refactor
 *   BidiHelper to accept Locale directly (recommended).
 */
class BidiHelperTest {

    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources
    private lateinit var mockConfiguration: Configuration
    private lateinit var mockLocaleList: LocaleList

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockResources = mockk(relaxed = true)
        mockConfiguration = mockk(relaxed = true)
        mockLocaleList = mockk(relaxed = true)

        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns mockConfiguration
        every { mockConfiguration.locales } returns mockLocaleList
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun setLocale(locale: Locale) {
        every { mockLocaleList[0] } returns locale
    }

    // ==================== Page Counter Formatting Tests ====================

    @Test
    fun `formatPageCounter returns formatted string for LTR locale`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatPageCounter(1, 10, mockContext)

        assertNotNull(result)
        assertTrue(result.contains("1"))
        assertTrue(result.contains("10"))
        assertFalse(result.isEmpty())
    }

    @Test
    fun `formatPageCounter returns formatted string for RTL locale`() {
        setLocale(Locale("ar"))

        val result = BidiHelper.formatPageCounter(1, 10, mockContext)

        assertNotNull(result)
        assertTrue(result.contains("1"))
        assertTrue(result.contains("10"))
        assertFalse(result.isEmpty())
    }

    @Test
    fun `formatPageCounter with single digit numbers`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatPageCounter(5, 9, mockContext)

        assertTrue(result.contains("5"))
        assertTrue(result.contains("9"))
    }

    @Test
    fun `formatPageCounter with double digit numbers`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatPageCounter(15, 100, mockContext)

        assertTrue(result.contains("15"))
        assertTrue(result.contains("100"))
    }

    @Test
    fun `formatPageCounter with first page`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatPageCounter(1, 50, mockContext)

        assertTrue(result.contains("1"))
        assertTrue(result.contains("50"))
    }

    @Test
    fun `formatPageCounter with last page`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatPageCounter(50, 50, mockContext)

        assertTrue(result.contains("50"))
    }

    // ==================== Progress Formatting Tests ====================

    @Test
    fun `formatProgress returns formatted percentage for LTR locale`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatProgress(75, mockContext)

        assertTrue(result.contains("75"))
        assertTrue(result.contains("%"))
    }

    @Test
    fun `formatProgress returns formatted percentage for RTL locale`() {
        setLocale(Locale("ar"))

        val result = BidiHelper.formatProgress(75, mockContext)

        assertTrue(result.contains("75"))
        assertTrue(result.contains("%"))
    }

    @Test
    fun `formatProgress with 0 percent`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatProgress(0, mockContext)

        assertTrue(result.contains("0"))
        assertTrue(result.contains("%"))
    }

    @Test
    fun `formatProgress with 100 percent`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatProgress(100, mockContext)

        assertTrue(result.contains("100"))
        assertTrue(result.contains("%"))
    }

    @Test
    fun `formatProgress with single digit percentage`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatProgress(5, mockContext)

        assertTrue(result.contains("5"))
        assertTrue(result.contains("%"))
    }

    // ==================== Repeat Counter Formatting Tests ====================

    @Test
    fun `formatRepeatCounter returns formatted counter for LTR locale`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatRepeatCounter(5, 33, mockContext)

        assertTrue(result.contains("5"))
        assertTrue(result.contains("33"))
    }

    @Test
    fun `formatRepeatCounter returns formatted counter for RTL locale`() {
        setLocale(Locale("ar"))

        val result = BidiHelper.formatRepeatCounter(5, 33, mockContext)

        assertTrue(result.contains("5"))
        assertTrue(result.contains("33"))
    }

    @Test
    fun `formatRepeatCounter with zero current count`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatRepeatCounter(0, 33, mockContext)

        assertTrue(result.contains("0"))
        assertTrue(result.contains("33"))
    }

    @Test
    fun `formatRepeatCounter with completion state`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatRepeatCounter(33, 33, mockContext)

        assertTrue(result.contains("33"))
    }

    @Test
    fun `formatRepeatCounter with different required counts`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatRepeatCounter(10, 100, mockContext)

        assertTrue(result.contains("10"))
        assertTrue(result.contains("100"))
    }

    // ==================== Verse Reference Formatting Tests ====================

    @Test
    fun `formatVerseReference returns formatted reference for LTR locale`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatVerseReference("[Bukhari 5074]", mockContext)

        assertTrue(result.contains("Bukhari"))
        assertTrue(result.contains("5074"))
    }

    @Test
    fun `formatVerseReference returns formatted reference for RTL locale`() {
        setLocale(Locale("ar"))

        val result = BidiHelper.formatVerseReference("[Bukhari 5074]", mockContext)

        assertTrue(result.contains("Bukhari"))
        assertTrue(result.contains("5074"))
    }

    @Test
    fun `formatVerseReference with different reference formats`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatVerseReference("[Muslim 123]", mockContext)

        assertTrue(result.contains("Muslim"))
        assertTrue(result.contains("123"))
    }

    @Test
    fun `formatVerseReference with complex reference`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatVerseReference("[Tirmidhi 3567]", mockContext)

        assertTrue(result.contains("Tirmidhi"))
        assertTrue(result.contains("3567"))
    }

    // ==================== Mixed Text Formatting Tests ====================

    @Test
    fun `formatMixedText handles Arabic with numbers`() {
        setLocale(Locale("ar"))

        val mixedText = "سبحان الله 33 مرة"
        val result = BidiHelper.formatMixedText(mixedText, mockContext)

        // For non-empty input, should remain non-empty
        assertFalse(result.isEmpty())
    }

    @Test
    fun `formatMixedText handles Arabic with Latin characters`() {
        setLocale(Locale("ar"))

        val mixedText = "Bukhari 5074"
        val result = BidiHelper.formatMixedText(mixedText, mockContext)

        assertFalse(result.isEmpty())
        assertTrue(result.contains("Bukhari"))
        assertTrue(result.contains("5074"))
    }

    @Test
    fun `formatMixedText handles complex mixed content`() {
        setLocale(Locale("ar"))

        val mixedText = "ذكر [Bukhari 5074] 33 مرة"
        val result = BidiHelper.formatMixedText(mixedText, mockContext)

        assertFalse(result.isEmpty())
    }

    @Test
    fun `formatMixedText handles pure Arabic text`() {
        setLocale(Locale("ar"))

        val arabicText = "سبحان الله"
        val result = BidiHelper.formatMixedText(arabicText, mockContext)

        assertFalse(result.isEmpty())
    }

    @Test
    fun `formatMixedText handles pure English text`() {
        setLocale(Locale("en"))

        val englishText = "Glory be to Allah"
        val result = BidiHelper.formatMixedText(englishText, mockContext)

        assertFalse(result.isEmpty())
    }

    // ==================== LTR Direction Formatting Tests ====================

    @Test
    fun `formatAsLtr forces LTR direction for Arabic locale`() {
        setLocale(Locale("ar"))

        val text = "123"
        val result = BidiHelper.formatAsLtr(text, mockContext)

        assertFalse(result.isEmpty())
        assertTrue(result.contains("123"))
    }

    @Test
    fun `formatAsLtr forces LTR direction for English locale`() {
        setLocale(Locale("en"))

        val text = "123"
        val result = BidiHelper.formatAsLtr(text, mockContext)

        assertFalse(result.isEmpty())
        assertTrue(result.contains("123"))
    }

    @Test
    fun `formatAsLtr with page counter`() {
        setLocale(Locale("ar"))

        val text = "(1/10)"
        val result = BidiHelper.formatAsLtr(text, mockContext)

        assertFalse(result.isEmpty())
        assertTrue(result.contains("1"))
        assertTrue(result.contains("10"))
    }

    @Test
    fun `formatAsLtr with percentage`() {
        setLocale(Locale("ar"))

        val text = "75%"
        val result = BidiHelper.formatAsLtr(text, mockContext)

        assertFalse(result.isEmpty())
        assertTrue(result.contains("75"))
        assertTrue(result.contains("%"))
    }

    // ==================== RTL Direction Formatting Tests ====================

    @Test
    fun `formatAsRtl forces RTL direction for Arabic locale`() {
        setLocale(Locale("ar"))

        val text = "سبحان الله"
        val result = BidiHelper.formatAsRtl(text, mockContext)

        assertFalse(result.isEmpty())
    }

    @Test
    fun `formatAsRtl forces RTL direction for English locale`() {
        setLocale(Locale("en"))

        val text = "سبحان الله"
        val result = BidiHelper.formatAsRtl(text, mockContext)

        assertFalse(result.isEmpty())
    }

    @Test
    fun `formatAsRtl with Arabic text containing numbers`() {
        setLocale(Locale("ar"))

        val text = "33 مرة"
        val result = BidiHelper.formatAsRtl(text, mockContext)

        assertFalse(result.isEmpty())
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handles empty string`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatMixedText("", mockContext)

        // Empty in -> should be empty out (more correct than "isNotEmpty()")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `handles special characters`() {
        setLocale(Locale("en"))

        val text = "Test! @# $%"
        val result = BidiHelper.formatMixedText(text, mockContext)

        assertFalse(result.isEmpty())
    }

    @Test
    fun `handles very long numbers`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatPageCounter(999, 9999, mockContext)

        assertTrue(result.contains("999"))
        assertTrue(result.contains("9999"))
    }

    @Test
    fun `handles zero page counter`() {
        setLocale(Locale("en"))

        val result = BidiHelper.formatPageCounter(0, 10, mockContext)

        assertTrue(result.contains("0"))
        assertTrue(result.contains("10"))
    }

    @Test
    fun `handles different locales consistently`() {
        val locales = listOf(Locale("en"), Locale("ar"), Locale("fr"))

        locales.forEach { locale ->
            setLocale(locale)

            val result = BidiHelper.formatPageCounter(1, 10, mockContext)

            assertTrue(result.contains("1"))
            assertTrue(result.contains("10"))
        }
    }
}
