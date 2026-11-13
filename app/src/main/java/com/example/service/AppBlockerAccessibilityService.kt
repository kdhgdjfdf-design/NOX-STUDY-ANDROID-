package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.example.ui.screens.focus.BlockedAppActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppBlockerAccessibilityService : AccessibilityService() {

    companion object {
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected = _isServiceConnected.asStateFlow()

        // Global flag controlled by FocusViewModel
        var isFocusActive: Boolean = false
        var blockedPackages: Set<String> = emptySet()

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
                if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isFocusActive || blockedPackages.isEmpty()) return
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName == packageName) return // Do not block NOX STUDY itself

        if (blockedPackages.contains(pkgName)) {
            val now = SystemClock.uptimeMillis()
            if (now - lastBlockedTime > 1500) { // prevent loop
                lastBlockedTime = now
                val intent = Intent(this, BlockedAppActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    putExtra("BLOCKED_PKG", pkgName)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {
        _isServiceConnected.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceConnected.value = false
    }
}
