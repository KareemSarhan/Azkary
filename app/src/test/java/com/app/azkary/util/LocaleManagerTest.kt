package com.app.azkary.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.app.azkary.data.prefs.AppLanguage
import com.app.azkary.data.prefs.UserPreferencesRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Unit tests for LocaleManager
 *
 * Tests cover:
 * - Language change functionality
 * - Locale retrieval
 * - RTL detection
 * - Language tag retrieval
 * - Arabic language detection
 */
class LocaleManagerTest {

    private lateinit var localeManager: LocaleManager
    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources
    private lateinit var mockConfiguration: Configuration
    private lateinit var mockUserPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        // Mock AppCompatDelegate static methods
        mockkStatic(AppCompatDelegate::class)

        // Mock Context
        mockContext = mockk()
        mockResources = mockk()
        mockConfiguration = mockk()

        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns mockConfiguration

        // Mock UserPreferencesRepository
        mockUserPreferencesRepository = mockk()

        // Create LocaleManager instance
        localeManager = LocaleManager(mockContext, mockUserPreferencesRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(AppCompatDelegate::class)
        unmockkAll()
    }

    // ==================== Language Change Tests ====================

    @Test
    fun `changeLanguage with 'en' saves English preference and sets locale`() = runTest {
        // Arrange
        every { mockUserPreferencesRepository.setAppLanguage(AppLanguage.ENGLISH) } just Runs
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.changeLanguage("en")

        // Assert
        verify { mockUserPreferencesRepository.setAppLanguage(AppLanguage.ENGLISH) }
        verify {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("en")
            )
        }
    }

    @Test
    fun `changeLanguage with 'ar' saves Arabic preference and sets locale`() = runTest {
        // Arrange
        every { mockUserPreferencesRepository.setAppLanguage(AppLanguage.ARABIC) } just Runs
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.changeLanguage("ar")

        // Assert
        verify { mockUserPreferencesRepository.setAppLanguage(AppLanguage.ARABIC) }
        verify {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("ar")
            )
        }
    }

    @Test
    fun `changeLanguage with 'EN' (uppercase) saves English preference`() = runTest {
        // Arrange
        every { mockUserPreferencesRepository.setAppLanguage(AppLanguage.ENGLISH) } just Runs
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.changeLanguage("EN")

        // Assert
        verify { mockUserPreferencesRepository.setAppLanguage(AppLanguage.ENGLISH) }
    }

    @Test
    fun `changeLanguage with 'AR' (uppercase) saves Arabic preference`() = runTest {
        // Arrange
        every { mockUserPreferencesRepository.setAppLanguage(AppLanguage.ARABIC) } just Runs
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.changeLanguage("AR")

        // Assert
        verify { mockUserPreferencesRepository.setAppLanguage(AppLanguage.ARABIC) }
    }

    @Test
    fun `changeLanguage with 'system' saves SYSTEM preference and clears locale`() = runTest {
        // Arrange
        every { mockUserPreferencesRepository.setAppLanguage(AppLanguage.SYSTEM) } just Runs
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.changeLanguage("system")

        // Assert
        verify { mockUserPreferencesRepository.setAppLanguage(AppLanguage.SYSTEM) }
        verify { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
    }

    @Test
    fun `changeLanguage with unknown value saves SYSTEM preference`() = runTest {
        // Arrange
        every { mockUserPreferencesRepository.setAppLanguage(AppLanguage.SYSTEM) } just Runs
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.changeLanguage("fr")

        // Assert
        verify { mockUserPreferencesRepository.setAppLanguage(AppLanguage.SYSTEM) }
    }

    @Test
    fun `changeLanguage with AppLanguage enum directly`() = runTest {
        // Arrange
        every { mockUserPreferencesRepository.setAppLanguage(AppLanguage.ARABIC) } just Runs
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.changeLanguage(AppLanguage.ARABIC)

        // Assert
        verify { mockUserPreferencesRepository.setAppLanguage(AppLanguage.ARABIC) }
        verify {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("ar")
            )
        }
    }

    // ==================== Locale Retrieval Tests ====================

    @Test
    fun `getCurrentLocale returns locale from AppCompatDelegate when available`() {
        // Arrange
        val expectedLocale = Locale("ar")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns expectedLocale

        // Act
        val result = localeManager.getCurrentLocale(mockContext)

        // Assert
        assertEquals(expectedLocale, result)
        verify { AppCompatDelegate.getApplicationLocales() }
        verify(exactly = 0) { mockContext.resources }
    }

    @Test
    fun `getCurrentLocale falls back to configuration when AppCompatDelegate returns empty`() {
        // Arrange
        val expectedLocale = Locale("en")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        val mockLocaleList = mockk<android.os.LocaleList>()

        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns true
        every { mockConfiguration.locales } returns mockLocaleList
        every { mockLocaleList[0] } returns expectedLocale

        // Act
        val result = localeManager.getCurrentLocale(mockContext)

        // Assert
        assertEquals(expectedLocale, result)
        verify { AppCompatDelegate.getApplicationLocales() }
        verify { mockContext.resources }
    }

    @Test
    fun `getCurrentLocale falls back to system default when both sources are null`() {
        // Arrange
        val systemDefault = Locale.getDefault()
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        val mockLocaleList = mockk<android.os.LocaleList>()

        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns true
        every { mockConfiguration.locales } returns mockLocaleList
        every { mockLocaleList[0] } returns null

        // Act
        val result = localeManager.getCurrentLocale(mockContext)

        // Assert
        assertEquals(systemDefault, result)
    }

    // ==================== RTL Detection Tests ====================

    @Test
    fun `isCurrentLocaleRtl returns true for Arabic locale`() {
        // Arrange
        val arabicLocale = Locale("ar")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns arabicLocale

        // Act
        val result = localeManager.isCurrentLocaleRtl(mockContext)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isCurrentLocaleRtl returns false for English locale`() {
        // Arrange
        val englishLocale = Locale("en")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns englishLocale

        // Act
        val result = localeManager.isCurrentLocaleRtl(mockContext)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isCurrentLocaleRtl returns false for French locale`() {
        // Arrange
        val frenchLocale = Locale("fr")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns frenchLocale

        // Act
        val result = localeManager.isCurrentLocaleRtl(mockContext)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isCurrentLocaleRtl returns true for Hebrew locale`() {
        // Arrange
        val hebrewLocale = Locale("iw") // Hebrew language code
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns hebrewLocale

        // Act
        val result = localeManager.isCurrentLocaleRtl(mockContext)

        // Assert
        assertTrue(result)
    }

    // ==================== Language Tag Tests ====================

    @Test
    fun `getCurrentLanguageTag returns 'en' for English locale`() {
        // Arrange
        val englishLocale = Locale("en")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns englishLocale

        // Act
        val result = localeManager.getCurrentLanguageTag(mockContext)

        // Assert
        assertEquals("en", result)
    }

    @Test
    fun `getCurrentLanguageTag returns 'ar' for Arabic locale`() {
        // Arrange
        val arabicLocale = Locale("ar")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns arabicLocale

        // Act
        val result = localeManager.getCurrentLanguageTag(mockContext)

        // Assert
        assertEquals("ar", result)
    }

    @Test
    fun `getCurrentLanguageTag returns 'fr' for French locale`() {
        // Arrange
        val frenchLocale = Locale("fr")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns frenchLocale

        // Act
        val result = localeManager.getCurrentLanguageTag(mockContext)

        // Assert
        assertEquals("fr", result)
    }

    // ==================== Arabic Language Detection Tests ====================

    @Test
    fun `isCurrentLanguageArabic returns true for Arabic locale`() {
        // Arrange
        val arabicLocale = Locale("ar")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns arabicLocale

        // Act
        val result = localeManager.isCurrentLanguageArabic(mockContext)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isCurrentLanguageArabic returns false for English locale`() {
        // Arrange
        val englishLocale = Locale("en")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns englishLocale

        // Act
        val result = localeManager.isCurrentLanguageArabic(mockContext)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isCurrentLanguageArabic returns false for French locale`() {
        // Arrange
        val frenchLocale = Locale("fr")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns frenchLocale

        // Act
        val result = localeManager.isCurrentLanguageArabic(mockContext)

        // Assert
        assertFalse(result)
    }

    // ==================== System Default Tests ====================

    @Test
    fun `setSystemDefault saves SYSTEM preference`() = runTest {
        // Arrange
        every { mockUserPreferencesRepository.setAppLanguage(AppLanguage.SYSTEM) } just Runs
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.setSystemDefault(mockContext)

        // Assert
        verify { mockUserPreferencesRepository.setAppLanguage(AppLanguage.SYSTEM) }
        verify { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
    }

    @Test
    fun `initializeLocale applies saved English locale`() = runTest {
        // Arrange
        coEvery { mockUserPreferencesRepository.appLanguage } returns flowOf(AppLanguage.ENGLISH)
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.initializeLocale()

        // Assert
        verify {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("en")
            )
        }
    }

    @Test
    fun `initializeLocale applies saved Arabic locale`() = runTest {
        // Arrange
        coEvery { mockUserPreferencesRepository.appLanguage } returns flowOf(AppLanguage.ARABIC)
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.initializeLocale()

        // Assert
        verify {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("ar")
            )
        }
    }

    @Test
    fun `initializeLocale clears locale when saved preference is SYSTEM`() = runTest {
        // Arrange
        coEvery { mockUserPreferencesRepository.appLanguage } returns flowOf(AppLanguage.SYSTEM)
        every { AppCompatDelegate.setApplicationLocales(any()) } just Runs

        // Act
        localeManager.initializeLocale()

        // Assert
        verify { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handles null locale from AppCompatDelegate gracefully`() {
        // Arrange
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns null
        every { mockConfiguration.locales } returns mockk(relaxed = true)

        // Act
        val result = localeManager.getCurrentLocale(mockContext)

        // Assert
        // Should fall back to system default
        assertEquals(Locale.getDefault(), result)
    }

    @Test
    fun `handles locale with country code correctly`() {
        // Arrange
        val localeWithCountry = Locale("en", "US")
        val mockLocaleListCompat = mockk<LocaleListCompat>()
        every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleListCompat
        every { mockLocaleListCompat.isEmpty } returns false
        every { mockLocaleListCompat[0] } returns localeWithCountry

        // Act
        val languageTag = localeManager.getCurrentLanguageTag(mockContext)

        // Assert
        assertEquals("en", languageTag)
        verify { mockLocaleListCompat[0] }
    }
}
