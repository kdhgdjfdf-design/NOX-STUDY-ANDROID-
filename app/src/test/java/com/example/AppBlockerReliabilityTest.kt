package com.example

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.service.AppBlockerAccessibilityService
import com.example.service.BlockerStateStore
import com.example.service.FocusSessionCoordinator
import com.example.ui.viewmodel.FocusMode
import com.example.ui.viewmodel.FocusState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppBlockerReliabilityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BlockerStateStore.clearAllForTesting(context)
        BlockerStateStore.bootCountProvider = { 10 }
        BlockerStateStore.currentTimeProvider = { 1_700_000_000_000L }
    }

    @After
    fun tearDown() {
        FocusSessionCoordinator.handleAppTaskRemoved(context)
        BlockerStateStore.clearAllForTesting(context)
        BlockerStateStore.bootCountProvider = { ctx ->
            try {
                Settings.Global.getInt(ctx.contentResolver, Settings.Global.BOOT_COUNT, -1)
            } catch (_: Exception) {
                -1
            }
        }
        BlockerStateStore.currentTimeProvider = { System.currentTimeMillis() }
    }

    @Test
    fun `blocker persists config and enforces across focus pause break and new session`() {
        val blockedSet = setOf("com.instagram.android", "com.google.android.youtube")
        BlockerStateStore.syncBlockerConfig(context, enabled = true, blockedPackages = blockedSet)

        // Idle -> must NOT block
        assertFalse(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))

        // Start session 1 -> MUST block blocked packages, NOT unblocked or self or systemui
        BlockerStateStore.setFocusSessionActive(
            context = context,
            mode = "POMODORO",
            phase = "FOCUS_RUNNING",
            expectedEndWallClockMs = 1_700_000_000_000L + 25 * 60_000L,
            processToken = "proc-1"
        )
        assertTrue(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))
        assertTrue(BlockerStateStore.shouldBlockPackage(context, "com.google.android.youtube"))
        assertFalse(BlockerStateStore.shouldBlockPackage(context, "com.wikipedia.org"))
        assertFalse(BlockerStateStore.shouldBlockPackage(context, context.packageName))

        // Pause -> must NOT block
        BlockerStateStore.setFocusSessionInactive(context, phase = "FOCUS_PAUSED")
        assertFalse(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))

        // Break -> must NOT block
        BlockerStateStore.setFocusSessionInactive(context, phase = "BREAK_RUNNING")
        assertFalse(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))

        // Start session 2 -> MUST block again cleanly without needing re-configuration
        BlockerStateStore.setFocusSessionActive(
            context = context,
            mode = "CUSTOM",
            phase = "FOCUS_RUNNING",
            expectedEndWallClockMs = 1_700_000_000_000L + 45 * 60_000L,
            processToken = "proc-1"
        )
        assertTrue(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))
        assertTrue(BlockerStateStore.shouldBlockPackage(context, "com.google.android.youtube"))
    }

    @Test
    fun `safe recovery clears orphaned blocking after process crash or force stop`() {
        BlockerStateStore.syncBlockerConfig(context, enabled = true, blockedPackages = setOf("com.whatsapp"))
        BlockerStateStore.setFocusSessionActive(
            context = context,
            mode = "FREE",
            phase = "FOCUS_RUNNING",
            expectedEndWallClockMs = 0L,
            processToken = "old-killed-process"
        )
        assertTrue(BlockerStateStore.shouldBlockPackage(context, "com.whatsapp"))

        // App restarts in a fresh process with a new token -> stale blocking MUST clear
        val recovered = BlockerStateStore.performSafeRecoveryIfNeeded(context, currentProcessToken = "new-process")
        assertTrue(recovered)
        assertFalse(BlockerStateStore.shouldBlockPackage(context, "com.whatsapp"))
        assertEquals("IDLE", BlockerStateStore.readSnapshot(context).sessionPhase)
    }

    @Test
    fun `safe recovery clears orphaned blocking after device reboot`() {
        BlockerStateStore.bootCountProvider = { 41 }
        BlockerStateStore.syncBlockerConfig(context, enabled = true, blockedPackages = setOf("com.zhiliaoapp.musically"))
        BlockerStateStore.setFocusSessionActive(
            context = context,
            mode = "FREE",
            phase = "FOCUS_RUNNING",
            expectedEndWallClockMs = 0L,
            processToken = NoxStudyApp.processToken
        )
        assertTrue(BlockerStateStore.shouldBlockPackage(context, "com.zhiliaoapp.musically"))

        // Device reboots (BOOT_COUNT increments)
        BlockerStateStore.bootCountProvider = { 42 }
        assertFalse(BlockerStateStore.shouldBlockPackage(context, "com.zhiliaoapp.musically"))
        assertFalse(BlockerStateStore.readSnapshot(context).sessionActive)
    }

    @Test
    fun `expired timed session automatically stops blocking`() {
        val start = 1_700_000_000_000L
        BlockerStateStore.currentTimeProvider = { start }
        BlockerStateStore.syncBlockerConfig(context, enabled = true, blockedPackages = setOf("com.instagram.android"))
        BlockerStateStore.setFocusSessionActive(
            context = context,
            mode = "POMODORO",
            phase = "FOCUS_RUNNING",
            expectedEndWallClockMs = start + 60_000L,
            processToken = NoxStudyApp.processToken
        )
        assertTrue(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))

        // Advance time past expected end
        BlockerStateStore.currentTimeProvider = { start + 65_000L }
        assertFalse(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))
        assertFalse(BlockerStateStore.readSnapshot(context).sessionActive)
    }

    @Test
    fun `coordinator retains focus state across FocusViewModel lifecycle and resets cleanly on stop`() {
        BlockerStateStore.syncBlockerConfig(context, enabled = true, blockedPackages = setOf("com.instagram.android"))
        BlockerStateStore.currentTimeProvider = { System.currentTimeMillis() }

        FocusSessionCoordinator.setMode(FocusMode.POMODORO)
        FocusSessionCoordinator.startSession(context)
        assertEquals(FocusState.RUNNING, FocusSessionCoordinator.focusState.value)
        assertTrue(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))

        // Transition to break turns blocking off, starting new session turns blocking back on
        FocusSessionCoordinator.startBreak(context, breakMinutes = 5)
        assertEquals(FocusState.BREAK_RUNNING, FocusSessionCoordinator.focusState.value)
        assertFalse(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))

        FocusSessionCoordinator.startSession(context)
        assertEquals(FocusState.RUNNING, FocusSessionCoordinator.focusState.value)
        assertTrue(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))

        FocusSessionCoordinator.stopSession(context, userTerminated = true)
        assertEquals(FocusState.IDLE, FocusSessionCoordinator.focusState.value)
        assertFalse(BlockerStateStore.shouldBlockPackage(context, "com.instagram.android"))
    }

    @Test
    fun `accessibility settings detection matches short and full component strings`() {
        val fullName = ComponentName(context, AppBlockerAccessibilityService::class.java).flattenToString()
        val shortName = ComponentName(context, AppBlockerAccessibilityService::class.java).flattenToShortString()

        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "com.other/.Service:$fullName"
        )
        assertTrue(AppBlockerAccessibilityService.isAccessibilitySettingsEnabled(context))

        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            shortName
        )
        assertTrue(AppBlockerAccessibilityService.isAccessibilitySettingsEnabled(context))

        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "com.other/.AnotherService"
        )
        assertFalse(AppBlockerAccessibilityService.isAccessibilitySettingsEnabled(context))
    }
}
