package com.lifeos.app.core.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lifeos.app.core.util.SettingsStore
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

/** Daily alarm scheduler. Uses exact alarms when Android permits them and falls back safely otherwise. */
object AlarmScheduler {
    private const val REQUEST_CODE = 7301
    private const val ACTION = "com.lifeos.app.action.DAILY_ALARM"

    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val trigger = nextTrigger(hour, minute)
        val pendingIntent = pendingIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    fun rescheduleFromSettings(context: Context) {
        kotlinx.coroutines.runBlocking {
            val store = SettingsStore(context)
            if (store.alarmEnabled.first()) scheduleDaily(context, store.alarmHour.first(), store.alarmMinute.first())
        }
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, AlarmReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun nextTrigger(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(hour.coerceIn(0, 23)).withMinute(minute.coerceIn(0, 59)).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
