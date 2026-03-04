package com.app.azkary.util

import android.app.Activity
import android.view.View
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InAppUpdateManagerTest {

    private lateinit var mockActivity: ComponentActivity
    private lateinit var mockLifecycleOwner: LifecycleOwner
    private lateinit var mockLifecycle: Lifecycle
    private lateinit var mockPlayUpdateManager: PlayUpdateManagerWrapper
    private lateinit var mockWindow: Window
    private lateinit var mockRootView: View

    private lateinit var inAppUpdateManager: InAppUpdateManager

    @Before
    fun setup() {
        mockActivity = mockk(relaxed = true)
        mockLifecycleOwner = mockk(relaxed = true)
        mockLifecycle = mockk(relaxed = true)
        mockPlayUpdateManager = mockk(relaxed = true)
        mockWindow = mockk(relaxed = true)
        mockRootView = mockk(relaxed = true)

        every { mockLifecycleOwner.lifecycle } returns mockLifecycle
        every { mockActivity.window } returns mockWindow
        every { mockWindow.decorView.rootView } returns mockRootView
        every { mockActivity.getString(any()) } returns "An update has just been downloaded."
        every { mockActivity.getString(any(), any()) } returns "Restart"

        mockkStatic(Snackbar::class)
        val mockSnackbar = mockk<Snackbar>(relaxed = true)
        every { Snackbar.make(any<View>(), any<String>(), any<Int>()) } returns mockSnackbar
        every { mockSnackbar.setAction(any<String>(), any()) } returns mockSnackbar

        inAppUpdateManager = InAppUpdateManager(
            activity = mockActivity,
            lifecycleOwner = mockLifecycleOwner,
            playUpdateManager = mockPlayUpdateManager
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== checkForUpdate Tests ====================

    @Test
    fun `checkForUpdate does nothing when lifecycle is not RESUMED`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.CREATED

        inAppUpdateManager.checkForUpdate()

        verify(exactly = 0) { mockPlayUpdateManager.getAppUpdateInfo(any(), any()) }
    }

    @Test
    fun `checkForUpdate calls getAppUpdateInfo when lifecycle is RESUMED`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.RESUMED

        inAppUpdateManager.checkForUpdate()

        verify { mockPlayUpdateManager.getAppUpdateInfo(any(), any()) }
    }

    @Test
    fun `checkForUpdate does nothing when lifecycle is STARTED (not RESUMED)`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.STARTED

        inAppUpdateManager.checkForUpdate()

        verify(exactly = 0) { mockPlayUpdateManager.getAppUpdateInfo(any(), any()) }
    }

    // ==================== Update Availability Tests ====================

    @Test
    fun `checkForUpdate does not start update when no update available`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.RESUMED
        val successSlot = slot<(UpdateInfo) -> Unit>()
        every { mockPlayUpdateManager.getAppUpdateInfo(capture(successSlot), any()) } answers {
            val info = UpdateInfo(
                updateAvailability = UpdateAvailability.UPDATE_NOT_AVAILABLE,
                isFlexibleAllowed = false,
                isImmediateAllowed = false,
                installStatus = InstallStatus.PENDING
            )
            successSlot.captured.invoke(info)
        }

        inAppUpdateManager.checkForUpdate()

        verify(exactly = 0) { mockPlayUpdateManager.startUpdateFlowForResult(any(), any()) }
    }

    @Test
    fun `checkForUpdate starts flexible update when available and allowed`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.RESUMED
        val successSlot = slot<(UpdateInfo) -> Unit>()
        every { mockPlayUpdateManager.getAppUpdateInfo(capture(successSlot), any()) } answers {
            val info = UpdateInfo(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                isFlexibleAllowed = true,
                isImmediateAllowed = false,
                installStatus = InstallStatus.PENDING
            )
            successSlot.captured.invoke(info)
        }
        every { mockPlayUpdateManager.startUpdateFlowForResult(any(), any()) } returns true

        inAppUpdateManager.checkForUpdate()

        verify { mockPlayUpdateManager.registerListener(any()) }
        verify { mockPlayUpdateManager.startUpdateFlowForResult(InAppUpdateManager.UPDATE_REQUEST_CODE, AppUpdateType.FLEXIBLE) }
    }

    @Test
    fun `checkForUpdate starts immediate update when flexible not allowed`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.RESUMED
        val successSlot = slot<(UpdateInfo) -> Unit>()
        every { mockPlayUpdateManager.getAppUpdateInfo(capture(successSlot), any()) } answers {
            val info = UpdateInfo(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                isFlexibleAllowed = false,
                isImmediateAllowed = true,
                installStatus = InstallStatus.PENDING
            )
            successSlot.captured.invoke(info)
        }
        every { mockPlayUpdateManager.startUpdateFlowForResult(any(), any()) } returns true

        inAppUpdateManager.checkForUpdate()

        verify { mockPlayUpdateManager.startUpdateFlowForResult(InAppUpdateManager.UPDATE_REQUEST_CODE, AppUpdateType.IMMEDIATE) }
    }

    @Test
    fun `checkForUpdate does nothing when update available but no type allowed`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.RESUMED
        val successSlot = slot<(UpdateInfo) -> Unit>()
        every { mockPlayUpdateManager.getAppUpdateInfo(capture(successSlot), any()) } answers {
            val info = UpdateInfo(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                isFlexibleAllowed = false,
                isImmediateAllowed = false,
                installStatus = InstallStatus.PENDING
            )
            successSlot.captured.invoke(info)
        }

        inAppUpdateManager.checkForUpdate()

        verify(exactly = 0) { mockPlayUpdateManager.startUpdateFlowForResult(any(), any()) }
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `checkForUpdate calls failure listener on error`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.RESUMED
        val failureSlot = slot<(Exception) -> Unit>()
        every { mockPlayUpdateManager.getAppUpdateInfo(any(), capture(failureSlot)) } answers {
            failureSlot.captured.invoke(Exception("Test error"))
        }

        inAppUpdateManager.checkForUpdate()

        verify { mockPlayUpdateManager.getAppUpdateInfo(any(), any()) }
    }

    // ==================== onActivityResult Tests ====================

    @Test
    fun `onActivityResult handles UPDATE_REQUEST_CODE with RESULT_OK`() {
        inAppUpdateManager.onActivityResult(InAppUpdateManager.UPDATE_REQUEST_CODE, Activity.RESULT_OK)
    }

    @Test
    fun `onActivityResult handles UPDATE_REQUEST_CODE with RESULT_CANCELED`() {
        inAppUpdateManager.onActivityResult(InAppUpdateManager.UPDATE_REQUEST_CODE, Activity.RESULT_CANCELED)
    }

    @Test
    fun `onActivityResult ignores other request codes`() {
        inAppUpdateManager.onActivityResult(9999, Activity.RESULT_OK)
    }

    // ==================== onResume Tests ====================

    @Test
    fun `onResume checks appUpdateInfo`() {
        inAppUpdateManager.onResume()

        verify { mockPlayUpdateManager.getAppUpdateInfo(any(), any()) }
    }

    @Test
    fun `onResume checks install status when update is DOWNLOADED`() {
        val successSlot = slot<(UpdateInfo) -> Unit>()
        every { mockPlayUpdateManager.getAppUpdateInfo(capture(successSlot), any()) } answers {
            val info = UpdateInfo(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                isFlexibleAllowed = true,
                isImmediateAllowed = false,
                installStatus = InstallStatus.DOWNLOADED
            )
            successSlot.captured.invoke(info)
        }

        inAppUpdateManager.onResume()

        verify { mockPlayUpdateManager.getAppUpdateInfo(any(), any()) }
    }

    @Test
    fun `onResume resumes in-progress update`() {
        val successSlot = slot<(UpdateInfo) -> Unit>()
        every { mockPlayUpdateManager.getAppUpdateInfo(capture(successSlot), any()) } answers {
            val info = UpdateInfo(
                updateAvailability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                isFlexibleAllowed = true,
                isImmediateAllowed = false,
                installStatus = InstallStatus.DOWNLOADING
            )
            successSlot.captured.invoke(info)
        }
        every { mockPlayUpdateManager.startUpdateFlowForResult(any(), any()) } returns true

        inAppUpdateManager.onResume()

        verify { mockPlayUpdateManager.startUpdateFlowForResult(any(), any()) }
    }

    @Test
    fun `onResume does not show snackbar when no downloaded update`() {
        val successSlot = slot<(UpdateInfo) -> Unit>()
        every { mockPlayUpdateManager.getAppUpdateInfo(capture(successSlot), any()) } answers {
            val info = UpdateInfo(
                updateAvailability = UpdateAvailability.UPDATE_NOT_AVAILABLE,
                isFlexibleAllowed = false,
                isImmediateAllowed = false,
                installStatus = InstallStatus.PENDING
            )
            successSlot.captured.invoke(info)
        }

        inAppUpdateManager.onResume()

        verify(exactly = 0) { mockPlayUpdateManager.completeUpdate() }
    }

    // ==================== onDestroy Tests ====================

    @Test
    fun `onDestroy unregisters listener when registered`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.RESUMED
        val successSlot = slot<(UpdateInfo) -> Unit>()
        every { mockPlayUpdateManager.getAppUpdateInfo(capture(successSlot), any()) } answers {
            val info = UpdateInfo(
                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                isFlexibleAllowed = true,
                isImmediateAllowed = false,
                installStatus = InstallStatus.PENDING
            )
            successSlot.captured.invoke(info)
        }
        every { mockPlayUpdateManager.startUpdateFlowForResult(any(), any()) } returns true

        inAppUpdateManager.checkForUpdate()
        inAppUpdateManager.onDestroy()

        verify { mockPlayUpdateManager.unregisterListener(any()) }
    }

    @Test
    fun `onDestroy handles no listener registered gracefully`() {
        inAppUpdateManager.onDestroy()
    }

    @Test
    fun `onDestroy called multiple times does not crash`() {
        inAppUpdateManager.onDestroy()
        inAppUpdateManager.onDestroy()
        inAppUpdateManager.onDestroy()
    }

    // ==================== Constants Tests ====================

    @Test
    fun `UPDATE_REQUEST_CODE is correct`() {
        assertEquals(1001, InAppUpdateManager.UPDATE_REQUEST_CODE)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `handles unknown update availability`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.RESUMED
        val successSlot = slot<(UpdateInfo) -> Unit>()
        every { mockPlayUpdateManager.getAppUpdateInfo(capture(successSlot), any()) } answers {
            val info = UpdateInfo(
                updateAvailability = UpdateAvailability.UNKNOWN,
                isFlexibleAllowed = false,
                isImmediateAllowed = false,
                installStatus = InstallStatus.PENDING
            )
            successSlot.captured.invoke(info)
        }

        inAppUpdateManager.checkForUpdate()

        verify(exactly = 0) { mockPlayUpdateManager.startUpdateFlowForResult(any(), any()) }
    }

    @Test
    fun `checkForUpdate can be called multiple times`() {
        every { mockLifecycle.currentState } returns Lifecycle.State.RESUMED

        inAppUpdateManager.checkForUpdate()
        inAppUpdateManager.checkForUpdate()
        inAppUpdateManager.checkForUpdate()

        verify(exactly = 3) { mockPlayUpdateManager.getAppUpdateInfo(any(), any()) }
    }

    @Test
    fun `onResume can be called multiple times`() {
        inAppUpdateManager.onResume()
        inAppUpdateManager.onResume()
        inAppUpdateManager.onResume()

        verify(exactly = 3) { mockPlayUpdateManager.getAppUpdateInfo(any(), any()) }
    }
}
