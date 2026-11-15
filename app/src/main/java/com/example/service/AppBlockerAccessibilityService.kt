package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.example.NoxStudyApp
import com.example.ui.screens.focus.BlockedAppActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected = _isServiceConnected.asStateFlow()

        @Volatile
        private var lastBlockedTime = 0L

        @Volatile
        private var lastBlockedPkg: String? = null

        private const val DEBOUNCE_SAME_PKG_MS = 1800L
        private const val DEBOUNCE_ANY_PKG_MS = 600L

        private val IGNORED_SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "android"
        )

        fun isAccessibilitySettingsEnabled(context: Context): Boolean {
            val expectedComponent = ComponentName(context, AppBlockerAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val parsed = ComponentName.unflattenFromString(componentNameString)
                if (parsed == expectedComponent) {
                    return true
                }
            }
            return false
        }

        fun isPermissionEffectivelyGranted(context: Context): Boolean {
            return _isServiceConnected.value || isAccessibilitySettingsEnabled(context)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true

        // Ensure stale blocking from reboot/crash is cleared, and re-sync persisted DataStore blocker config
        BlockerStateStore.performSafeRecoveryIfNeeded(this)
        serviceScope.launch {
            try {
                val app = applicationContext as? NoxStudyApp ?: return@launch
                app.preferencesRepository.syncBlockerPreferencesToStore()
            } catch (_: Exception) {
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName == packageName || IGNORED_SYSTEM_PACKAGES.contains(pkgName)) return

        // Never stack duplicate block screens while BlockedAppActivity is already visible
        if (BlockedAppActivity.isCurrentlyVisible) return

        // Synchronous authoritative check backed by SharedPreferences + process/boot/expiry guards
        if (!BlockerStateStore.shouldBlockPackage(this, pkgName, packageName)) return

        val now = SystemClock.uptimeMillis()
        val elapsedSinceLast = now - lastBlockedTime
        val isSamePkgWithinWindow = (lastBlockedPkg == pkgName && elapsedSinceLast < DEBOUNCE_SAME_PKG_MS)
        val isRapidTrigger = elapsedSinceLast < DEBOUNCE_ANY_PKG_MS
        if (isSamePkgWithinWindow || isRapidTrigger) return

        lastBlockedTime = now
        lastBlockedPkg = pkgName

        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(BlockedAppActivity.EXTRA_BLOCKED_PKG, pkgName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        _isServiceConnected.value = false
    }

    override fun onUnbind(intent: Intent?): Boolean {
        _isServiceConnected.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceConnected.value = false
        serviceScope.cancel()
    }
}
