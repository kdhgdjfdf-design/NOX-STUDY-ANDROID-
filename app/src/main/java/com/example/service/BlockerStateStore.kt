package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.example.NoxStudyApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BlockerSnapshot(
    val appBlockerEnabled: Boolean = false,
    val blockedPackages: Set<String> = emptySet(),
    val sessionActive: Boolean = false,
    val sessionPhase: String = "IDLE",
    val sessionMode: String = "POMODORO",
    val expectedEndWallClockMs: Long = 0L,
    val lastHeartbeatWallClockMs: Long = 0L,
    val savedBootCount: Int = -1,
    val activeProcessToken: String = ""
)

object BlockerStateStore {

    private const val PREFS_NAME = "nox_blocker_state"
    private const val KEY_BLOCKER_ENABLED = "app_blocker_enabled"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_SESSION_ACTIVE = "session_active"
    private const val KEY_SESSION_PHASE = "session_phase"
    private const val KEY_SESSION_MODE = "session_mode"
    private const val KEY_EXPECTED_END_MS = "expected_end_wall_clock_ms"
    private const val KEY_HEARTBEAT_MS = "last_heartbeat_wall_clock_ms"
    private const val KEY_BOOT_COUNT = "saved_boot_count"
    private const val KEY_PROCESS_TOKEN = "active_process_token"

    // Overridable in unit/Robolectric tests
    internal var bootCountProvider: (Context) -> Int = { ctx ->
        try {
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.BOOT_COUNT, -1)
        } catch (_: Exception) {
            -1
        }
    }

    internal var currentTimeProvider: () -> Long = { System.currentTimeMillis() }

    private val _snapshotFlow = MutableStateFlow(BlockerSnapshot())
    val snapshotFlow: StateFlow<BlockerSnapshot> = _snapshotFlow.asStateFlow()

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun readSnapshot(context: Context): BlockerSnapshot {
        val p = prefs(context)
        val snap = BlockerSnapshot(
            appBlockerEnabled = p.getBoolean(KEY_BLOCKER_ENABLED, false),
            blockedPackages = p.getStringSet(KEY_BLOCKED_PACKAGES, emptySet())?.toSet() ?: emptySet(),
            sessionActive = p.getBoolean(KEY_SESSION_ACTIVE, false),
            sessionPhase = p.getString(KEY_SESSION_PHASE, "IDLE") ?: "IDLE",
            sessionMode = p.getString(KEY_SESSION_MODE, "POMODORO") ?: "POMODORO",
            expectedEndWallClockMs = p.getLong(KEY_EXPECTED_END_MS, 0L),
            lastHeartbeatWallClockMs = p.getLong(KEY_HEARTBEAT_MS, 0L),
            savedBootCount = p.getInt(KEY_BOOT_COUNT, -1),
            activeProcessToken = p.getString(KEY_PROCESS_TOKEN, "") ?: ""
        )
        _snapshotFlow.value = snap
        return snap
    }

    fun syncBlockerConfig(context: Context, enabled: Boolean, blockedPackages: Set<String>) {
        prefs(context).edit()
            .putBoolean(KEY_BLOCKER_ENABLED, enabled)
            .putStringSet(KEY_BLOCKED_PACKAGES, blockedPackages.toSet())
            .commit()
        readSnapshot(context)
    }

    fun setFocusSessionActive(
        context: Context,
        mode: String,
        phase: String = "FOCUS_RUNNING",
        expectedEndWallClockMs: Long? = null,
        processToken: String = NoxStudyApp.processToken
    ) {
        val now = currentTimeProvider()
        val bootCount = bootCountProvider(context)
        prefs(context).edit()
            .putBoolean(KEY_SESSION_ACTIVE, true)
            .putString(KEY_SESSION_PHASE, phase)
            .putString(KEY_SESSION_MODE, mode)
            .putLong(KEY_EXPECTED_END_MS, expectedEndWallClockMs ?: 0L)
            .putLong(KEY_HEARTBEAT_MS, now)
            .putInt(KEY_BOOT_COUNT, bootCount)
            .putString(KEY_PROCESS_TOKEN, processToken)
            .commit()
        readSnapshot(context)
    }

    fun setFocusSessionInactive(context: Context, phase: String = "IDLE") {
        prefs(context).edit()
            .putBoolean(KEY_SESSION_ACTIVE, false)
            .putString(KEY_SESSION_PHASE, phase)
            .putLong(KEY_EXPECTED_END_MS, 0L)
            .putString(KEY_PROCESS_TOKEN, "")
            .commit()
        readSnapshot(context)
    }

    fun updateHeartbeat(context: Context, expectedEndWallClockMs: Long? = null) {
        val editor = prefs(context).edit()
            .putLong(KEY_HEARTBEAT_MS, currentTimeProvider())
        if (expectedEndWallClockMs != null) {
            editor.putLong(KEY_EXPECTED_END_MS, expectedEndWallClockMs)
        }
        editor.apply()
    }

    /**
     * Clears orphaned active blocking if the device rebooted, the previous process crashed/was killed,
     * or a timed session already expired while the app wasn't running.
     * Returns true if stale active blocking was cleared.
     */
    fun performSafeRecoveryIfNeeded(
        context: Context,
        currentProcessToken: String = NoxStudyApp.processToken
    ): Boolean {
        val snap = readSnapshot(context)
        if (!snap.sessionActive) return false

        val now = currentTimeProvider()
        val currentBootCount = bootCountProvider(context)

        val rebooted = snap.savedBootCount != -1 &&
            currentBootCount != -1 &&
            snap.savedBootCount != currentBootCount

        val processRestarted = snap.activeProcessToken.isNotEmpty() &&
            snap.activeProcessToken != currentProcessToken

        val timedExpired = snap.expectedEndWallClockMs > 0L &&
            now > snap.expectedEndWallClockMs + 5_000L

        if (rebooted || processRestarted || timedExpired) {
            setFocusSessionInactive(context, phase = "IDLE")
            return true
        }
        return false
    }

    /**
     * Authoritative synchronous check called from AccessibilityService.onAccessibilityEvent
     * and tests. Guarantees stale sessions never remain blocked after expiry or reboot.
     */
    fun shouldBlockPackage(
        context: Context,
        packageName: String,
        ownPackageName: String = context.packageName
    ): Boolean {
        if (packageName.isBlank() || packageName == ownPackageName) return false

        val snap = readSnapshot(context)
        if (!snap.appBlockerEnabled || !snap.sessionActive || snap.sessionPhase != "FOCUS_RUNNING") {
            return false
        }
        if (snap.blockedPackages.isEmpty()) return false

        val now = currentTimeProvider()
        val currentBootCount = bootCountProvider(context)
        val rebooted = snap.savedBootCount != -1 &&
            currentBootCount != -1 &&
            snap.savedBootCount != currentBootCount
        if (rebooted) {
            setFocusSessionInactive(context, phase = "IDLE")
            return false
        }

        if (snap.expectedEndWallClockMs > 0L && now > snap.expectedEndWallClockMs) {
            setFocusSessionInactive(context, phase = "IDLE")
            return false
        }

        return snap.blockedPackages.contains(packageName)
    }

    fun clearAllForTesting(context: Context) {
        prefs(context).edit().clear().commit()
        _snapshotFlow.value = BlockerSnapshot()
    }
}
