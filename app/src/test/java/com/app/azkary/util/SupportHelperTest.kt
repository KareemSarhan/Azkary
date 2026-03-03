package com.app.azkary.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone

/**
 * Unit tests for SupportHelper
 *
 * Tests cover:
 * - Device info gathering (app version, build, device model, etc.)
 * - Email intent building with correct structure
 * - Email body formatting with device info
 * - Safe intent launching
 * - String resource retrieval
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU], application = android.app.Application::class)
class SupportHelperTest {

    private lateinit var supportHelper: SupportHelper
    private lateinit var mockContext: Context
    private lateinit var mockLocaleManager: LocaleManager
    private lateinit var realContext: Context

    @Before
    fun setup() {
        // Get real context for integration-style tests
        realContext = RuntimeEnvironment.getApplication()

        // Create mocks for isolated tests
        mockContext = mockk(relaxed = true)
        mockLocaleManager = mockk()

        // Setup basic mocks
        every { mockLocaleManager.getCurrentLocale(any()) } returns Locale("en", "US")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== DeviceInfo Tests ====================

    @Test
    fun `DeviceInfo data class stores all values correctly`() {
        // Arrange
        val deviceInfo = SupportHelper.DeviceInfo(
            appVersion = "2.0.0",
            buildNumber = 42,
            deviceManufacturer = "Google",
            deviceModel = "Pixel 7",
            androidVersion = "13",
            androidSdk = 33,
            locale = "en-US",
            timezone = "America/New_York"
        )

        // Assert
        assertEquals("2.0.0", deviceInfo.appVersion)
        assertEquals(42, deviceInfo.buildNumber)
        assertEquals("Google", deviceInfo.deviceManufacturer)
        assertEquals("Pixel 7", deviceInfo.deviceModel)
        assertEquals("13", deviceInfo.androidVersion)
        assertEquals(33, deviceInfo.androidSdk)
        assertEquals("en-US", deviceInfo.locale)
        assertEquals("America/New_York", deviceInfo.timezone)
    }

    @Test
    fun `DeviceInfo equals and hashCode work correctly`() {
        // Arrange
        val info1 = SupportHelper.DeviceInfo(
            appVersion = "1.0",
            buildNumber = 1,
            deviceManufacturer = "Test",
            deviceModel = "Test",
            androidVersion = "13",
            androidSdk = 33,
            locale = "en",
            timezone = "UTC"
        )
        val info2 = SupportHelper.DeviceInfo(
            appVersion = "1.0",
            buildNumber = 1,
            deviceManufacturer = "Test",
            deviceModel = "Test",
            androidVersion = "13",
            androidSdk = 33,
            locale = "en",
            timezone = "UTC"
        )
        val info3 = SupportHelper.DeviceInfo(
            appVersion = "2.0",
            buildNumber = 2,
            deviceManufacturer = "Test",
            deviceModel = "Test",
            androidVersion = "13",
            androidSdk = 33,
            locale = "en",
            timezone = "UTC"
        )

        // Assert
        assertEquals(info1, info2)
        assertEquals(info1.hashCode(), info2.hashCode())
        assertNotEquals(info1, info3)
    }

    @Test
    fun `DeviceInfo copy function works correctly`() {
        // Arrange
        val original = SupportHelper.DeviceInfo(
            appVersion = "1.0",
            buildNumber = 1,
            deviceManufacturer = "Test",
            deviceModel = "Test",
            androidVersion = "13",
            androidSdk = 33,
            locale = "en",
            timezone = "UTC"
        )

        // Act
        val copy = original.copy(appVersion = "2.0", buildNumber = 2)

        // Assert
        assertEquals("2.0", copy.appVersion)
        assertEquals(2, copy.buildNumber)
        assertEquals(original.deviceManufacturer, copy.deviceManufacturer)
    }

    // ==================== getDeviceInfo Tests ====================

    @Test
    fun `getDeviceInfo extracts app version from PackageInfo`() {
        // Arrange
        val mockPackageInfo = PackageInfo().apply {
            versionName = "2.1.0"
            longVersionCode = 100L
        }
        val mockPackageManager = mockk<PackageManager>()

        every { mockContext.packageName } returns "com.app.azkary"
        every { mockContext.packageManager } returns mockPackageManager
        every {
            mockPackageManager.getPackageInfo("com.app.azkary", 0)
        } returns mockPackageInfo

        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val deviceInfo = supportHelper.getDeviceInfo()

        // Assert
        assertEquals("2.1.0", deviceInfo.appVersion)
        assertEquals(100, deviceInfo.buildNumber)
    }

    @Test
    fun `getDeviceInfo handles null versionName`() {
        // Arrange
        val mockPackageInfo = PackageInfo().apply {
            versionName = null
            longVersionCode = 50L
        }
        val mockPackageManager = mockk<PackageManager>()

        every { mockContext.packageName } returns "com.app.azkary"
        every { mockContext.packageManager } returns mockPackageManager
        every {
            mockPackageManager.getPackageInfo("com.app.azkary", 0)
        } returns mockPackageInfo

        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val deviceInfo = supportHelper.getDeviceInfo()

        // Assert
        assertEquals("unknown", deviceInfo.appVersion)
    }

    @Test
    fun `getDeviceInfo includes device manufacturer and model`() {
        // Arrange
        val mockPackageInfo = PackageInfo().apply {
            versionName = "1.0"
            longVersionCode = 1L
        }
        val mockPackageManager = mockk<PackageManager>()

        every { mockContext.packageName } returns "com.app.azkary"
        every { mockContext.packageManager } returns mockPackageManager
        every {
            mockPackageManager.getPackageInfo("com.app.azkary", 0)
        } returns mockPackageInfo

        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val deviceInfo = supportHelper.getDeviceInfo()

        // Assert
        assertNotNull(deviceInfo.deviceManufacturer)
        assertNotNull(deviceInfo.deviceModel)
        // Manufacturer and model should match Build values
        assertEquals(Build.MANUFACTURER, deviceInfo.deviceManufacturer)
        assertEquals(Build.MODEL, deviceInfo.deviceModel)
    }

    @Test
    fun `getDeviceInfo includes Android version and SDK`() {
        // Arrange
        val mockPackageInfo = PackageInfo().apply {
            versionName = "1.0"
            longVersionCode = 1L
        }
        val mockPackageManager = mockk<PackageManager>()

        every { mockContext.packageName } returns "com.app.azkary"
        every { mockContext.packageManager } returns mockPackageManager
        every {
            mockPackageManager.getPackageInfo("com.app.azkary", 0)
        } returns mockPackageInfo

        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val deviceInfo = supportHelper.getDeviceInfo()

        // Assert
        assertEquals(Build.VERSION.RELEASE, deviceInfo.androidVersion)
        assertEquals(Build.VERSION.SDK_INT, deviceInfo.androidSdk)
    }

    @Test
    fun `getDeviceInfo includes locale from LocaleManager`() {
        // Arrange
        val mockPackageInfo = PackageInfo().apply {
            versionName = "1.0"
            longVersionCode = 1L
        }
        val mockPackageManager = mockk<PackageManager>()
        val arabicLocale = Locale("ar", "EG")

        every { mockContext.packageName } returns "com.app.azkary"
        every { mockContext.packageManager } returns mockPackageManager
        every {
            mockPackageManager.getPackageInfo("com.app.azkary", 0)
        } returns mockPackageInfo
        every { mockLocaleManager.getCurrentLocale(mockContext) } returns arabicLocale

        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val deviceInfo = supportHelper.getDeviceInfo()

        // Assert
        assertEquals("ar-EG", deviceInfo.locale)
    }

    @Test
    fun `getDeviceInfo includes timezone`() {
        // Arrange
        val mockPackageInfo = PackageInfo().apply {
            versionName = "1.0"
            longVersionCode = 1L
        }
        val mockPackageManager = mockk<PackageManager>()
        val testTimezone = TimeZone.getTimeZone("Asia/Tokyo")

        every { mockContext.packageName } returns "com.app.azkary"
        every { mockContext.packageManager } returns mockPackageManager
        every {
            mockPackageManager.getPackageInfo("com.app.azkary", 0)
        } returns mockPackageInfo

        // Mock TimeZone
        mockkStatic(TimeZone::class)
        every { TimeZone.getDefault() } returns testTimezone

        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val deviceInfo = supportHelper.getDeviceInfo()

        // Assert
        assertEquals("Asia/Tokyo", deviceInfo.timezone)

        unmockkStatic(TimeZone::class)
    }

    // ==================== buildEmailIntent Tests ====================

    @Test
    fun `buildEmailIntent creates intent with ACTION_SENDTO`() {
        // Arrange
        setupBasicMocks()
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val intent = supportHelper.buildEmailIntent()

        // Assert
        assertEquals(Intent.ACTION_SENDTO, intent.action)
    }

    @Test
    fun `buildEmailIntent includes mailto scheme in data`() {
        // Arrange
        setupBasicMocks()
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val intent = supportHelper.buildEmailIntent()

        // Assert
        assertNotNull(intent.data)
        assertTrue(intent.data.toString().startsWith("mailto:"))
    }

    @Test
    fun `buildEmailIntent includes support email address`() {
        // Arrange
        setupBasicMocks()
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val intent = supportHelper.buildEmailIntent()

        // Assert
        val dataString = intent.data.toString()
        assertTrue(dataString.contains("azkary@hearo.support"))
    }

    @Test
    fun `buildEmailIntent includes subject parameter`() {
        // Arrange
        setupBasicMocks()
        every { mockContext.getString(any()) } returns "Test Subject"
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val intent = supportHelper.buildEmailIntent()

        // Assert
        val dataString = intent.data.toString()
        assertTrue(dataString.contains("subject="))
    }

    @Test
    fun `buildEmailIntent includes body parameter`() {
        // Arrange
        setupBasicMocks()
        every { mockContext.getString(any()) } returns "Test"
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val intent = supportHelper.buildEmailIntent()

        // Assert
        val dataString = intent.data.toString()
        assertTrue(dataString.contains("body="))
    }

    @Test
    fun `buildEmailIntent adds FLAG_ACTIVITY_NEW_TASK flag`() {
        // Arrange
        setupBasicMocks()
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val intent = supportHelper.buildEmailIntent()

        // Assert
        val flags = intent.flags
        assertTrue(flags and Intent.FLAG_ACTIVITY_NEW_TASK == Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `buildEmailIntent encodes special characters in subject`() {
        // Arrange
        setupBasicMocks()
        every { mockContext.getString(R.string.support_email_subject) } returns "Test & Support"
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val intent = supportHelper.buildEmailIntent()

        // Assert
        val dataString = intent.data.toString()
        // & should be encoded as %26
        assertTrue(dataString.contains("%26") || !dataString.contains("Test & Support"))
    }

    @Test
    fun `buildEmailIntent encodes special characters in body`() {
        // Arrange
        setupBasicMocks()
        every { mockContext.getString(any()) } returns "Line 1\nLine 2"
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val intent = supportHelper.buildEmailIntent()

        // Assert
        val dataString = intent.data.toString()
        // Newlines should be encoded
        assertFalse(dataString.contains("\n"))
    }

    // ==================== launchIntentSafely Tests ====================

    @Test
    fun `launchIntentSafely returns true when intent launches successfully`() {
        // Arrange
        setupBasicMocks()
        every { mockContext.startActivity(any()) } just Runs
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        val intent = Intent(Intent.ACTION_VIEW)

        // Act
        val result = supportHelper.launchIntentSafely(intent)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `launchIntentSafely returns false when ActivityNotFoundException`() {
        // Arrange
        setupBasicMocks()
        every { mockContext.startActivity(any()) } throws ActivityNotFoundException()
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        val intent = Intent(Intent.ACTION_VIEW)

        // Act
        val result = supportHelper.launchIntentSafely(intent)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `launchIntentSafely returns false on generic Exception`() {
        // Arrange
        setupBasicMocks()
        every { mockContext.startActivity(any()) } throws RuntimeException("Generic error")
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        val intent = Intent(Intent.ACTION_VIEW)

        // Act
        val result = supportHelper.launchIntentSafely(intent)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `launchIntentSafely handles SecurityException`() {
        // Arrange
        setupBasicMocks()
        every { mockContext.startActivity(any()) } throws SecurityException("Permission denied")
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        val intent = Intent(Intent.ACTION_VIEW)

        // Act
        val result = supportHelper.launchIntentSafely(intent)

        // Assert
        assertFalse(result)
    }

    // ==================== getString Tests ====================

    @Test
    fun `getString returns string from context resources`() {
        // Arrange
        val testString = "Test String Value"
        every { mockContext.getString(R.string.support_email_subject) } returns testString
        setupBasicMocks()
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        val result = supportHelper.getString(R.string.support_email_subject)

        // Assert
        assertEquals(testString, result)
    }

    @Test
    fun `getString calls context getString with correct resource id`() {
        // Arrange
        setupBasicMocks()
        every { mockContext.getString(123) } returns "Test"
        supportHelper = SupportHelper(mockContext, mockLocaleManager)

        // Act
        supportHelper.getString(123)

        // Assert
        verify { mockContext.getString(123) }
    }

    // ==================== Integration-style Tests ====================

    @Test
    fun `getDeviceInfo with real context returns valid info`() {
        // Arrange - use real context for integration test
        val realLocaleManager = mockk<LocaleManager>()
        every { realLocaleManager.getCurrentLocale(any()) } returns Locale.getDefault()
        supportHelper = SupportHelper(realContext, realLocaleManager)

        // Act
        val deviceInfo = supportHelper.getDeviceInfo()

        // Assert
        assertNotNull(deviceInfo.appVersion)
        assertTrue(deviceInfo.buildNumber > 0)
        assertNotNull(deviceInfo.deviceManufacturer)
        assertNotNull(deviceInfo.deviceModel)
        assertNotNull(deviceInfo.androidVersion)
        assertTrue(deviceInfo.androidSdk > 0)
        assertNotNull(deviceInfo.locale)
        assertNotNull(deviceInfo.timezone)
    }

    // ==================== Helper Methods ====================

    private fun setupBasicMocks() {
        val mockPackageInfo = PackageInfo().apply {
            versionName = "1.0.0"
            longVersionCode = 1L
        }
        val mockPackageManager = mockk<PackageManager>()

        every { mockContext.packageName } returns "com.app.azkary"
        every { mockContext.packageManager } returns mockPackageManager
        every {
            mockPackageManager.getPackageInfo("com.app.azkary", 0)
        } returns mockPackageInfo
    }
}
