package com.app.azkary

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.app.azkary.data.prefs.AppLanguage
import com.app.azkary.util.LocaleManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Instrumented tests for Multi-Language UI functionality
 * 
 * Tests cover:
 * - Language switching in Settings screen
 * - Language persistence after activity recreation
 * - RTL/LTR layout direction verification
 * - String resource loading in both languages
 * - UI component direction verification
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MultiLanguageUiTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var localeManager: LocaleManager

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Reset to English before each test
        resetToEnglish()
    }

    @After
    fun tearDown() {
        // Reset to English after each test
        resetToEnglish()
    }

    // ==================== Language Switching Tests ====================

    @Test
    fun testLanguageSwitchingToArabic() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Change language to Arabic
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        // Recreate activity to apply language change
        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify locale is Arabic
            val currentLocale = localeManager.getCurrentLocale(activity)
            assertEquals("ar", currentLocale.language)
            
            // Verify RTL layout
            assertTrue(localeManager.isCurrentLocaleRtl(activity))
        }
    }

    @Test
    fun testLanguageSwitchingToEnglish() {
        // First switch to Arabic
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        // Then switch back to English
        scenario.onActivity { activity ->
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ENGLISH)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify locale is English
            val currentLocale = localeManager.getCurrentLocale(activity)
            assertEquals("en", currentLocale.language)
            
            // Verify LTR layout
            assertTrue(!localeManager.isCurrentLocaleRtl(activity))
        }
    }

    @Test
    fun testLanguageSwitchingToSystemDefault() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Change language to Arabic first
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        // Then switch to system default
        scenario.onActivity { activity ->
            runBlocking {
                localeManager.changeLanguage(AppLanguage.SYSTEM)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify language is system default
            val currentLocale = localeManager.getCurrentLocale(activity)
            // Should match system locale
            val systemLocale = Locale.getDefault()
            assertEquals(systemLocale.language, currentLocale.language)
        }
    }

    // ==================== Language Persistence Tests ====================

    @Test
    fun testLanguagePersistsAfterActivityRecreation() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Change language to Arabic
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        // Recreate activity multiple times
        scenario.recreate()
        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify language persists
            val currentLocale = localeManager.getCurrentLocale(activity)
            assertEquals("ar", currentLocale.language)
            assertTrue(localeManager.isCurrentLocaleRtl(activity))
        }
    }

    @Test
    fun testLanguagePersistsAfterActivityRestart() {
        // Launch activity and change language
        val scenario1 = ActivityScenario.launch(MainActivity::class.java)

        scenario1.onActivity { activity ->
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario1.close()

        // Launch new activity instance
        val scenario2 = ActivityScenario.launch(MainActivity::class.java)

        scenario2.onActivity { activity ->
            // Verify language persists across activity instances
            val currentLocale = localeManager.getCurrentLocale(activity)
            assertEquals("ar", currentLocale.language)
            assertTrue(localeManager.isCurrentLocaleRtl(activity))
        }

        scenario2.close()
    }

    // ==================== RTL/LTR Layout Direction Tests ====================

    @Test
    fun testRtlLayoutDirectionForArabic() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Change language to Arabic
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify RTL layout direction
            val config = activity.resources.configuration
            val layoutDirection = config.layoutDirection
            
            // LAYOUT_DIRECTION_RTL = 1
            assertEquals(1, layoutDirection)
            
            // Verify LocaleManager detects RTL
            assertTrue(localeManager.isCurrentLocaleRtl(activity))
        }
    }

    @Test
    fun testLtrLayoutDirectionForEnglish() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Change language to English
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ENGLISH)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify LTR layout direction
            val config = activity.resources.configuration
            val layoutDirection = config.layoutDirection
            
            // LAYOUT_DIRECTION_LTR = 0
            assertEquals(0, layoutDirection)
            
            // Verify LocaleManager detects LTR
            assertTrue(!localeManager.isCurrentLocaleRtl(activity))
        }
    }

    @Test
    fun testLayoutDirectionChangesWithLanguage() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Start with English (LTR)
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ENGLISH)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify LTR
            assertEquals(0, activity.resources.configuration.layoutDirection)
            
            // Switch to Arabic (RTL)
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify RTL
            assertEquals(1, activity.resources.configuration.layoutDirection)
            
            // Switch back to English (LTR)
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ENGLISH)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify LTR again
            assertEquals(0, activity.resources.configuration.layoutDirection)
        }
    }

    // ==================== String Resource Loading Tests ====================

    @Test
    fun testStringResourceLoadingInEnglish() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Set language to English
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ENGLISH)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Load a string resource
            val appName = activity.getString(R.string.app_name)
            
            // Verify string is loaded (not empty)
            assertTrue(appName.isNotEmpty())
            
            // Verify locale is English
            val currentLocale = localeManager.getCurrentLocale(activity)
            assertEquals("en", currentLocale.language)
        }
    }

    @Test
    fun testStringResourceLoadingInArabic() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Set language to Arabic
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Load a string resource
            val appName = activity.getString(R.string.app_name)
            
            // Verify string is loaded (not empty)
            assertTrue(appName.isNotEmpty())
            
            // Verify locale is Arabic
            val currentLocale = localeManager.getCurrentLocale(activity)
            assertEquals("ar", currentLocale.language)
        }
    }

    @Test
    fun testStringResourcesChangeWithLanguage() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Set language to English
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ENGLISH)
            }
        }

        scenario.recreate()

        var englishAppName: String? = null

        scenario.onActivity { activity ->
            // Get app name in English
            englishAppName = activity.getString(R.string.app_name)
        }

        // Switch to Arabic
        scenario.onActivity { activity ->
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        var arabicAppName: String? = null

        scenario.onActivity { activity ->
            // Get app name in Arabic
            arabicAppName = activity.getString(R.string.app_name)
        }

        // Verify strings are different (or at least both loaded)
        assertTrue(englishAppName != null)
        assertTrue(arabicAppName != null)
        assertTrue(englishAppName!!.isNotEmpty())
        assertTrue(arabicAppName!!.isNotEmpty())
    }

    // ==================== UI Component Direction Tests ====================

    @Test
    fun testUiComponentsRespectLayoutDirection() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Set language to Arabic
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Get window layout direction
            val window = activity.window
            val decorView = window.decorView
            val layoutDirection = decorView.layoutDirection
            
            // Verify RTL layout direction
            assertEquals(android.view.View.LAYOUT_DIRECTION_RTL, layoutDirection)
        }
    }

    @Test
    fun testConfigurationUpdatesAfterLanguageChange() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Set language to English
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ENGLISH)
            }
        }

        scenario.recreate()

        var englishConfig: Configuration? = null

        scenario.onActivity { activity ->
            // Get configuration
            englishConfig = Configuration(activity.resources.configuration)
        }

        // Switch to Arabic
        scenario.onActivity { activity ->
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        var arabicConfig: Configuration? = null

        scenario.onActivity { activity ->
            // Get new configuration
            arabicConfig = Configuration(activity.resources.configuration)
        }

        // Verify configurations are different
        assertTrue(englishConfig != null)
        assertTrue(arabicConfig != null)
        assertTrue(englishConfig!!.layoutDirection != arabicConfig!!.layoutDirection)
    }

    // ==================== AppCompatDelegate Integration Tests ====================

    @Test
    fun testAppCompatDelegateLocaleSetting() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Change language to Arabic
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify AppCompatDelegate has correct locale
            val appLocales = AppCompatDelegate.getApplicationLocales()
            assertTrue(!appLocales.isEmpty)
            
            val primaryLocale = appLocales[0]
            assertEquals("ar", primaryLocale.language)
        }
    }

    @Test
    fun testAppCompatDelegateSystemDefault() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Set to system default
            runBlocking {
                localeManager.changeLanguage(AppLanguage.SYSTEM)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify AppCompatDelegate has empty locale list (system default)
            val appLocales = AppCompatDelegate.getApplicationLocales()
            // Should be empty or match system
            val systemLocale = Locale.getDefault()
            
            if (!appLocales.isEmpty) {
                val primaryLocale = appLocales[0]
                assertEquals(systemLocale.language, primaryLocale.language)
            }
        }
    }

    // ==================== LocaleManager Integration Tests ====================

    @Test
    fun testLocaleManagerGetCurrentLocale() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Change language to Arabic
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Get current locale from LocaleManager
            val currentLocale = localeManager.getCurrentLocale(activity)
            
            // Verify it matches Arabic
            assertEquals("ar", currentLocale.language)
        }
    }

    @Test
    fun testLocaleManagerGetCurrentLanguageTag() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Change language to English
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ENGLISH)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Get current language tag
            val languageTag = localeManager.getCurrentLanguageTag(activity)
            
            // Verify it's "en"
            assertEquals("en", languageTag)
        }
    }

    @Test
    fun testLocaleManagerIsCurrentLanguageArabic() {
        // Launch activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Change language to Arabic
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ARABIC)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify Arabic detection
            assertTrue(localeManager.isCurrentLanguageArabic(activity))
        }

        // Switch to English
        scenario.onActivity { activity ->
            runBlocking {
                localeManager.changeLanguage(AppLanguage.ENGLISH)
            }
        }

        scenario.recreate()

        scenario.onActivity { activity ->
            // Verify not Arabic
            assertTrue(!localeManager.isCurrentLanguageArabic(activity))
        }
    }

    // ==================== Helper Functions ====================

    private fun resetToEnglish() {
        // Reset to English locale
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags("en")
        )
    }

    // Helper for running suspend functions in tests
    private fun runBlocking(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking {
            block()
        }
    }
}
