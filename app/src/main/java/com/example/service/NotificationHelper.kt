package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_REMINDERS = "nox_study_reminders"
    const val CHANNEL_AUDIO = "nox_study_audio"
    const val CHANNEL_FOCUS = "nox_study_focus"
    const val CHANNEL_FOCUS_ONGOING = "nox_study_focus_ongoing"
    const val EXTRA_NAVIGATE_TO_FOCUS = "extra_navigate_to_focus"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "تذكيرات الدراسة والمواعيد",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات مواعيد مراجعة الدروس، تسليم الواجبات، والاختبارات"
                enableVibration(true)
            }

            val audioChannel = NotificationChannel(
                CHANNEL_AUDIO,
                "مشغل الموسيقى الهادئة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "التحكم في تشغيل الموسيقى الخلفية"
            }

            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS,
                "جلسات التركيز والاستراحة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات انتهاء فترات التركيز وبداية/نهاية الاستراحة"
                enableVibration(true)
            }

            val focusOngoingChannel = NotificationChannel(
                CHANNEL_FOCUS_ONGOING,
                "مؤقت التركيز الجاري",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "يعرض حالة مؤقت التركيز وحجب التطبيقات أثناء الجلسة"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(audioChannel)
            notificationManager.createNotificationChannel(focusChannel)
            notificationManager.createNotificationChannel(focusOngoingChannel)
        }
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String,
        message: String,
        channelId: String = CHANNEL_REMINDERS
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (channelId == CHANNEL_FOCUS) {
                putExtra(EXTRA_NAVIGATE_TO_FOCUS, true)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Handled if permission denied
        }
    }

    fun scheduleAlarm(
        context: Context,
        requestCode: Int,
        triggerTimeMillis: Long,
        title: String,
        message: String
    ) {
        if (triggerTimeMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "com.aistudio.noxstudy.ACTION_REMINDER"
            putExtra("EXTRA_ID", requestCode)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "com.aistudio.noxstudy.ACTION_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
