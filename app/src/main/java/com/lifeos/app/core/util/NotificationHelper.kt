package com.lifeos.app.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Central notification handling for Task/Habit reminders (Section 9/11).
 * All reminders originate from data the user themselves set (a due date, a
 * habit reminder time) — LifeOS never notifies about anything the user
 * didn't explicitly schedule.
 */
object NotificationHelper {
    const val CHANNEL_ID = "lifeos_reminders"
    private const val CHANNEL_NAME = "LifeOS Reminders"
    const val ALARM_CHANNEL_ID = "lifeos_alarm"
    private const val ALARM_CHANNEL_NAME = "LifeOS Alarm"
    const val ALARM_NOTIFICATION_ID = 7302

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for your tasks and habits"
            }
            manager.createNotificationChannel(channel)
            val alarmChannel = NotificationChannel(ALARM_CHANNEL_ID, ALARM_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Math challenge alarms"
                setSound(null, null)
                enableVibration(true)
            }
            manager.createNotificationChannel(alarmChannel)
        }
    }

    fun showAlarmNotification(context: Context) {
        ensureChannel(context)
        val hasPostPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            PermissionManager.hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        if (!hasPostPermission) return
        val intent = android.content.Intent(context, com.lifeos.app.ui.settings.AlarmChallengeActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = android.app.PendingIntent.getActivity(
            context, ALARM_NOTIFICATION_ID, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("LifeOS alarm")
            .setContentText("Solve the math challenge to stop the alarm")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .build()
        NotificationManagerCompat.from(context).notify(ALARM_NOTIFICATION_ID, notification)
    }

    fun cancelAlarmNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(ALARM_NOTIFICATION_ID)
    }

    fun showReminder(context: Context, notificationId: Int, title: String, body: String) {
        ensureChannel(context)
        val hasPostPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            PermissionManager.hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        if (!hasPostPermission) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // swap for a branded icon asset
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasPostPermission) {
                notify(notificationId, notification)
            }
        }
    }
}
