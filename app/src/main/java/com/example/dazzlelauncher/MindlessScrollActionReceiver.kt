package com.example.dazzlelauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MindlessScrollActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        when (intent?.action) {
            ACTION_SNOOZE -> {
                prefs.edit()
                    .putLong("mindless_scroll_snoozed_until", System.currentTimeMillis() + 5 * 60_000L)
                    .apply()
            }
            ACTION_CLOSE -> {
                prefs.edit().remove("mindless_scroll_snoozed_until").apply()
                context.startActivity(
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(7021)
    }

    companion object {
        const val ACTION_SNOOZE = "com.example.dazzlelauncher.action.MINDLESS_SNOOZE"
        const val ACTION_CLOSE = "com.example.dazzlelauncher.action.MINDLESS_CLOSE"
    }
}
