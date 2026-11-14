package com.example.service

import android.accessibilityservice.AccessibilityService
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    companion object {
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected = _isServiceConnected.asStateFlow()

        // The focus session is controlled by FocusViewModel.
        // These values are refreshed from persistent preferences whenever
        // Android reconnects the accessibility service.
        @Volatile var isFocusActive: Boolean = false
        @Volatile var blockedPackages: Set<String> = emptySet()

        private var lastBlockedPackage: String? = null
        private var lastBlockedTime = 0L

        fun isAccessibilitySettingsEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${AppBlockerAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true)) return true
            }
            return false
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true

        // A bound AccessibilityService can be recreated by Android while the
        // app process is still alive. Rehydrate the selected package list so
        // the service does not remain connected with an empty/stale cache.
        serviceScope.launch {
            runCatching {
                val prefs = (application as NoxStudyApp)
                    .preferencesRepository.userPreferencesFlow.first()
                blockedPackages = if (prefs.appBlockerEnabled) prefs.blockedPackages else emptySet()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isFocusActive || blockedPackages.isEmpty()) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName == packageName) return

        if (blockedPackages.contains(pkgName)) {
            val now = SystemClock.uptimeMillis()
            // Debounce duplicate events for the same app only. A global
            // debounce could let a different blocked app open immediately
            // after another one was intercepted.
            if (pkgName != lastBlockedPackage || now - lastBlockedTime > 700L) {
                lastBlockedPackage = pkgName
                lastBlockedTime = now
                val intent = Intent(this, BlockedAppActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                    putExtra("BLOCKED_PKG", pkgName)
                }
                runCatching { startActivity(intent) }
            }
        }
    }

    override fun onInterrupt() {
        _isServiceConnected.value = false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        _isServiceConnected.value = false
        super.onDestroy()
    }
}
