package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ui.viewmodel.FocusState
import java.util.Locale

class FocusTimerService : Service() {

    companion object {
        const val NOTIFICATION_ID = 4201
        const val ACTION_START_FOCUS = "com.aistudio.noxstudy.ACTION_START_FOCUS"
        const val ACTION_START_BREAK = "com.aistudio.noxstudy.ACTION_START_BREAK"
        const val ACTION_UPDATE_NOTIFICATION = "com.aistudio.noxstudy.ACTION_UPDATE_FOCUS_NOTIFICATION"
        const val ACTION_STOP_SERVICE = "com.aistudio.noxstudy.ACTION_STOP_FOCUS_SERVICE"

        fun refreshOngoingNotification(context: Context) {
            val state = FocusSessionCoordinator.focusState.value
            if (state == FocusState.IDLE) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.notify(NOTIFICATION_ID, buildNotification(context, state))
        }

        private fun buildNotification(context: Context, state: FocusState): Notification {
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NotificationHelper.EXTRA_NAVIGATE_TO_FOCUS, true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                tapIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val snap = BlockerStateStore.readSnapshot(context)
            val blockerActive = snap.appBlockerEnabled && snap.blockedPackages.isNotEmpty() && state == FocusState.RUNNING

            val (title, seconds) = when (state) {
                FocusState.RUNNING -> {
                    val label = if (blockerActive) "جلسة التركيز جارية • الحجب نشط 🛡️" else "جلسة التركيز جارية ⏳"
                    label to FocusSessionCoordinator.remainingSeconds.value
                }
                FocusState.PAUSED -> "جلسة التركيز متوقفة مؤقتاً ⏸️" to FocusSessionCoordinator.remainingSeconds.value
                FocusState.BREAK_RUNNING -> "وقت الاستراحة الهادئة ☕" to FocusSessionCoordinator.breakRemainingSeconds.value
                FocusState.BREAK_PAUSED -> "الاستراحة متوقفة مؤقتاً ☕" to FocusSessionCoordinator.breakRemainingSeconds.value
                FocusState.IDLE -> "NOX STUDY 📚" to 0
            }

            val mins = seconds / 60
            val secs = seconds % 60
            val formattedTime = String.format(Locale.US, "%02d:%02d", mins, secs)
            val contentText = when (state) {
                FocusState.RUNNING -> "الوقت الحالي: $formattedTime • ركّز على إنجازك"
                FocusState.PAUSED -> "متوقف عند $formattedTime • انقر للعودة"
                FocusState.BREAK_RUNNING, FocusState.BREAK_PAUSED -> "متبقي من الاستراحة: $formattedTime • الحجب متوقف"
                FocusState.IDLE -> "جلسة الدراسة"
            }

            return NotificationCompat.Builder(context, NotificationHelper.CHANNEL_FOCUS_ONGOING)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(contentText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_FOCUS, ACTION_START_BREAK, ACTION_UPDATE_NOTIFICATION, null -> {
                val state = FocusSessionCoordinator.focusState.value
                if (state == FocusState.IDLE) {
                    stopForegroundCompat()
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundCompat(buildNotification(this, state))
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // User swiped the app away from Recents: release blocking immediately so device never stays blocked
        FocusSessionCoordinator.handleAppTaskRemoved(this)
        stopForegroundCompat()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (FocusSessionCoordinator.focusState.value != FocusState.RUNNING) {
            BlockerStateStore.setFocusSessionInactive(this, phase = "IDLE")
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopForegroundCompat() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {
        }
    }
}
