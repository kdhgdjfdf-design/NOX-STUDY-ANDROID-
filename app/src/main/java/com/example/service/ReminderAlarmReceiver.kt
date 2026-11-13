package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("EXTRA_ID", 1001)
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "تذكير NOX STUDY 📚"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "حان موعد جلسة الدراسة أو المراجعة!"

        NotificationHelper.showNotification(
            context = context,
            id = id,
            title = title,
            message = message,
            channelId = NotificationHelper.CHANNEL_REMINDERS
        )
    }
}
