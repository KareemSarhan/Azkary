package com.app.azkary.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Unit tests for ArabicNormalizer
 *
 * Tests cover:
 * - Removing Arabic diacritics (tashkeel/harakat)
 * - Removing whitespace
 * - Removing punctuation
 * - Handling null input
 * - Handling empty strings
 * - Various Arabic text combinations
 */
@RunWith(Parameterized::class)
class ArabicNormalizerParameterizedTest(
    private val testName: String,
    private val input: String?,
    private val expected: String
) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any?>> = listOf(
            // Null and empty cases
            arrayOf("Null input returns empty string", null, ""),
            arrayOf("Empty string returns empty string", "", ""),
            arrayOf("Whitespace only returns empty string", "   ", ""),

            // Basic Arabic without diacritics
            arrayOf("Simple Arabic no change", "بسم الله", "بسمالله"),
            arrayOf("Arabic word", "محمد", "محمد"),
            arrayOf("Arabic phrase", "السلام عليكم", "السلامعليكم"),

            // With diacritics (tashkeel/harakat)
            arrayOf("Fatha", "بَسم", "بسم"),
            arrayOf("Damma", "بُسم", "بسم"),
            arrayOf("Kasra", "بِسم", "بسم"),
            arrayOf("Shadda", "بسمّ", "بسم"),
            arrayOf("Sukun", "بسمْ", "بسم"),
            arrayOf("Combined diacritics", "بِسْمِ", "بسم"),
            arrayOf("Full basmala with tashkeel", "بِسْمِ اللَّهِ", "بسمالله"),

            // With whitespace variations
            arrayOf("Single space", "hello world", "helloworld"),
            arrayOf("Multiple spaces", "hello   world", "helloworld"),
            arrayOf("Tab character", "hello\tworld", "helloworld"),
            arrayOf("Newline character", "hello\nworld", "helloworld"),
            arrayOf("Mixed whitespace", "hello \t\n world", "helloworld"),

            // With punctuation
            arrayOf("Period", "hello.", "hello"),
            arrayOf("Comma", "hello,", "hello"),
            arrayOf("Exclamation", "hello!", "hello"),
            arrayOf("Question mark", "hello?", "hello"),
            arrayOf("Colon", "hello:", "hello"),
            arrayOf("Semicolon", "hello;", "hello"),
            arrayOf("Quotes", "\"hello\"", "hello"),
            arrayOf("Parentheses", "(hello)", "hello"),
            arrayOf("Brackets", "[hello]", "hello"),
            arrayOf("Braces", "{hello}", "hello"),
            arrayOf("Mixed punctuation", "hello, world!", "helloworld"),

            // Arabic with punctuation
            arrayOf("Arabic with comma", "السلام، عليكم", "السلامعليكم"),
            arrayOf("Arabic with question mark", "كيف حالك؟", "كيفحالك"),

            // Numbers (should be preserved)
            arrayOf("Numbers preserved", "123", "123"),
            arrayOf("Mixed with numbers", "hello123", "hello123"),
            arrayOf("Arabic with numbers", "سورة 1", "سورة1"),

            // English text
            arrayOf("English lowercase", "hello world", "helloworld"),
            arrayOf("English uppercase", "HELLO WORLD", "HELLOWORLD"),
            arrayOf("English mixed case", "Hello World", "HelloWorld"),

            // Complex combinations
            arrayOf("Arabic + diacritics + spaces", "بِسْمِ اللَّهِ الرَّحْمَنِ", "بسماللهالرحمن"),
            arrayOf("Full sentence", "السَّلَامُ عَلَيْكُمْ!", "السلامعليكم"),
            arrayOf("Mixed Arabic English", "hello بالعربية", "helloبالعربية"),
        )
    }

    @Test
    fun testNormalize() {
        // Act
        val result = ArabicNormalizer.normalize(input)

        // Assert
        assertEquals("Failed for test: $testName", expected, result)
    }
}

class ArabicNormalizerTest {

    // ==================== Diacritic Range Tests ====================

    @Test
    fun `normalize removes all tashkeel characters`() {
        // Arrange - All tashkeel characters from range \u064B-\u065F
        val tashkeelChars = listOf(
            '\u064B', // Fathatan
            '\u064C', // Dammatan
            '\u064D', // Kasratan
            '\u064E', // Fatha
            '\u064F', // Damma
            '\u0650', // Kasra
            '\u0651', // Shadda
            '\u0652', // Sukun
            '\u0653', // Maddah
            '\u0654', // Hamza Above
            '\u0655', // Hamza Below
            '\u0656', // Subscript Alef
            '\u0657', // Inverted Damma
            '\u0658', // Mark Noon Ghunna
            '\u0659', // Zwarakay
            '\u065A', // Vowel Sign Small V Above
            '\u065B', // Vowel Sign Inverted Small V Above
            '\u065C', // Vowel Sign Dot Below
            '\u065D', // Reversed Damma
            '\u065E', // Fatha with Two Dots
            '\u065F', // Wavy Hamza Below
        )

        tashkeelChars.forEach { char ->
            // Act
            val result = ArabicNormalizer.normalize("م${char}حمد")

            // Assert
            assertEquals("Failed for char: $char (\\u${char.code.toString(16).uppercase()})", "محمد", result)
        }
    }

    @Test
    fun `normalize removes sukun character`() {
        // Arrange - \u0652 is Sukun
        val input = "سْلام"

        // Act
        val result = ArabicNormalizer.normalize(input)

        // Assert
        assertEquals("سلام", result)
    }

    @Test
    fun `normalize removes dagger alif`() {
        // Arrange - \u0670 is Dagger Alif (Superscript Alef)
        val input = "اللَّه"

        // Act
        val result = ArabicNormalizer.normalize(input)

        // Assert
        assertEquals("الله", result)
    }

    @Test
    fun `normalize removes quran annotation marks`() {
        // Arrange - \u06D6-\u06ED are Quranic annotation signs
        val quranMarks = listOf(
            '\u06D6', // Small High Ligature Sad with Lam with Alef Maksura
            '\u06D7', // Small High Ligature Qaf with Lam with Alef Maksura
            '\u06D8', // Small High Meem Initial Form
            '\u06D9', // Small High Lam Alef
            '\u06DA', // Small High Jeem
            '\u06DB', // Small High Three Dots
            '\u06DC', // Small High Seen
            '\u06DD', // End of Ayah
            '\u06DE', // Start of Rub El Hizb
            '\u06DF', // Small High Rounded Zero
            '\u06E0', // Small High Upright Rectangular Zero
            '\u06E1', // Small High Dotless Head of Khah
            '\u06E2', // Small High Meem Isolated Form
            '\u06E3', // Small Low Seen
            '\u06E4', // Small High Maddah
            '\u06E5', // Small Waw
            '\u06E6', // Small Ya
            '\u06E7', // Small High Yeh
            '\u06E8', // Small High Noon
            '\u06E9', // Empty Centre Low Stop
            '\u06EA', // Empty Centre High Stop
            '\u06EB', // Round High Stop with Filled Centre
            '\u06EC', // Round Low Stop with Filled Centre
            '\u06ED', // Small Low Meem
        )

        quranMarks.forEach { char ->
            // Act
            val result = ArabicNormalizer.normalize("ب${char}سم")

            // Assert
            assertEquals("Failed for char: $char (\\u${char.code.toString(16).uppercase()})", "بسم", result)
        }
    }

    // ==================== Whitespace Tests ====================

    @Test
    fun `normalize removes all types of whitespace`() {
        // Arrange
        val whitespaceChars = listOf(
            ' ',      // Space
            '\t',     // Tab
            '\n',     // Newline
            '\r',     // Carriage return
            '\u00A0', // Non-breaking space
        )

        whitespaceChars.forEach { char ->
            // Act
            val result = ArabicNormalizer.normalize("hello${char}world")

            // Assert
            assertEquals("Failed for whitespace char: ${char.code}", "helloworld", result)
        }
    }

    @Test
    fun `normalize handles multiple consecutive spaces`() {
        // Arrange
        val input = "a   b    c"

        // Act
        val result = ArabicNormalizer.normalize(input)

        // Assert
        assertEquals("abc", result)
    }

    // ==================== Punctuation Tests ====================

    @Test
    fun `normalize removes Arabic punctuation`() {
        // Arrange
        val arabicPunctuation = listOf(
            '؟',  // Arabic question mark
            '،',  // Arabic comma
            '؛',  // Arabic semicolon
            '«',  // Arabic left quotation mark
            '»',  // Arabic right quotation mark
        )

        arabicPunctuation.forEach { char ->
            // Act
            val result = ArabicNormalizer.normalize("text${char}more")

            // Assert
            assertEquals("textmore", result)
        }
    }

    @Test
    fun `normalize removes all punctuation categories`() {
        // Arrange
        val punctuations = listOf(
            '.', ',', '!', '?', ':', ';',
            '"', '\'', '-', '_', '(', ')',
            '[', ']', '{', '}', '<', '>',
            '/', '\\', '|', '@', '#', '$',
            '%', '^', '&', '*', '+', '=',
            '`', '~'
        )

        punctuations.forEach { char ->
            // Act
            val result = ArabicNormalizer.normalize("a${char}b")

            // Assert
            assertEquals("ab", result)
        }
    }

    // ==================== Symbol Tests ====================

    @Test
    fun `normalize removes symbols`() {
        // Arrange
        val symbols = listOf(
            '©', '®', '™', '§', '¶',
            '†', '‡', '•', '·', '◊'
        )

        symbols.forEach { char ->
            // Act
            val result = ArabicNormalizer.normalize("text${char}more")

            // Assert
            assertEquals("textmore", result)
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `normalize handles only diacritics`() {
        // Arrange - string with only diacritics
        val input = "َُِّْ"

        // Act
        val result = ArabicNormalizer.normalize(input)

        // Assert
        assertEquals("", result)
    }

    @Test
    fun `normalize handles only whitespace`() {
        // Arrange
        val input = "   \t\n  "

        // Act
        val result = ArabicNormalizer.normalize(input)

        // Assert
        assertEquals("", result)
    }

    @Test
    fun `normalize handles only punctuation`() {
        // Arrange
        val input = ".,!?;:'\""

        // Act
        val result = ArabicNormalizer.normalize(input)

        // Assert
        assertEquals("", result)
    }

    @Test
    fun `normalize handles mixed script text`() {
        // Arrange - Arabic + English + numbers + diacritics
        val input = "بِسْمِ (Bismi) Allah 123!"

        // Act
        val result = ArabicNormalizer.normalize(input)

        // Assert
        assertEquals("بسمBismiAllah123", result)
    }

    @Test
    fun `normalize handles long text`() {
        // Arrange - Long Arabic text with tashkeel
        val input = "إِنَّا أَنزَلْنَاهُ قُرْآنًا عَرَبِيًّا لَّعَلَّكُمْ تَعْقِلُونَ"

        // Act
        val result = ArabicNormalizer.normalize(input)

        // Assert
        assertEquals("إناأنزلناهقرآناعربيالعلكمتعقلون", result)
    }

    @Test
    fun `normalize preserves Arabic letters`() {
        // Arrange - All basic Arabic letters without diacritics
        val arabicLetters = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"

        // Act
        val result = ArabicNormalizer.normalize(arabicLetters)

        // Assert
        assertEquals(arabicLetters, result)
    }

    @Test
    fun `normalize handles empty string after normalization`() {
        // Arrange - only characters that will be removed
        val input = "   َُِ   !!!   "

        // Act
        val result = ArabicNormalizer.normalize(input)

        // Assert
        assertEquals("", result)
    }

    // ==================== normalizeForSearch Tests ====================

    @Test
    fun `normalizeForSearch removes diacritics and lowercases`() {
        // Arrange
        val input = "بِسْمِ اللَّهِ"

        // Act
        val result = ArabicNormalizer.normalizeForSearch(input)

        // Assert
        assertEquals("بسم الله", result)
    }

    @Test
    fun `normalizeForSearch handles null input`() {
        // Act
        val result = ArabicNormalizer.normalizeForSearch(null)

        // Assert
        assertEquals("", result)
    }

    @Test
    fun `normalizeForSearch lowercases English text`() {
        // Arrange
        val input = "HELLO World"

        // Act
        val result = ArabicNormalizer.normalizeForSearch(input)

        // Assert
        assertEquals("hello world", result)
    }

    @Test
    fun `normalizeForSearch normalizes whitespace`() {
        // Arrange
        val input = "hello   \t\n  world"

        // Act
        val result = ArabicNormalizer.normalizeForSearch(input)

        // Assert
        assertEquals("hello world", result)
    }

    @Test
    fun `normalizeForSearch trims whitespace`() {
        // Arrange
        val input = "  hello world  "

        // Act
        val result = ArabicNormalizer.normalizeForSearch(input)

        // Assert
        assertEquals("hello world", result)
    }

    // ==================== fuzzyMatch Tests ====================

    @Test
    fun `fuzzyMatch returns false for null text`() {
        // Act
        val result = ArabicNormalizer.fuzzyMatch("test", null)

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `fuzzyMatch returns false for blank text`() {
        // Act
        val result = ArabicNormalizer.fuzzyMatch("test", "   ")

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `fuzzyMatch returns true for blank query`() {
        // Act
        val result = ArabicNormalizer.fuzzyMatch("", "some text")

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `fuzzyMatch matches exact substring`() {
        // Act
        val result = ArabicNormalizer.fuzzyMatch("الله", "بسم الله الرحمن")

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `fuzzyMatch matches case insensitive`() {
        // Act
        val result = ArabicNormalizer.fuzzyMatch("HELLO", "hello world")

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `fuzzyMatch ignores diacritics in both query and text`() {
        // Act & Assert
        assertEquals(true, ArabicNormalizer.fuzzyMatch("بسم", "بِسْمِ اللَّهِ"))
        assertEquals(true, ArabicNormalizer.fuzzyMatch("بِسْمِ", "بسم الله"))
        assertEquals(true, ArabicNormalizer.fuzzyMatch("الله", "بِسْمِ اللَّهِ"))
    }

    @Test
    fun `fuzzyMatch matches similar strings with Levenshtein`() {
        // Act - "سبحان" vs "سبحن" (missing alef) should match with threshold 0.7
        val result = ArabicNormalizer.fuzzyMatch("سبحن", "سبحان", 0.6f)

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `fuzzyMatch matches English with typo`() {
        // Act - "glory" vs "glor" (missing letter)
        val result = ArabicNormalizer.fuzzyMatch("glor", "glory", 0.7f)

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `fuzzyMatch rejects dissimilar strings below threshold`() {
        // Act - "abc" vs "xyz" are very different
        val result = ArabicNormalizer.fuzzyMatch("abc", "xyz", 0.7f)

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `fuzzyMatch handles Arabic transliteration search`() {
        // Act - searching "subhan" should match "subhanallah"
        val result = ArabicNormalizer.fuzzyMatch("subhan", "subhanallah")

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `fuzzyMatch matches partial Arabic word`() {
        // Act
        val result = ArabicNormalizer.fuzzyMatch("رحمن", "الرحمن الرحيم")

        // Assert
        assertEquals(true, result)
    }

    @Test
    fun `fuzzyMatch with high threshold requires closer match`() {
        // Act - "glory" vs "gloy" (missing 'r') = 4/5 = 0.8 similarity
        val resultHigh = ArabicNormalizer.fuzzyMatch("gloy", "glory", 0.9f)
        val resultLow = ArabicNormalizer.fuzzyMatch("gloy", "glory", 0.7f)

        // Assert
        assertEquals(false, resultHigh)
        assertEquals(true, resultLow)
    }
}
