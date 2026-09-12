package com.lifeos.app.core.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lifeos.app.core.util.DailyAlarm
import com.lifeos.app.core.util.SettingsStore
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

/** Daily math-alarm scheduler. Supports multiple independent times without changing Room. */
object AlarmScheduler {
    private const val REQUEST_CODE_BASE = 7301
    private const val ACTION = "com.lifeos.app.action.DAILY_ALARM"
    private const val EXTRA_HOUR = "extra_hour"
    private const val EXTRA_MINUTE = "extra_minute"

    fun scheduleDaily(context: Context, hour: Int, minute: Int) = schedule(context, DailyAlarm(hour, minute))

    fun scheduleAll(context: Context, alarms: List<DailyAlarm>) {
        alarms.distinctBy { it.minutesSinceMidnight }.forEach { schedule(context, it) }
    }

    fun replaceDaily(context: Context, previous: List<DailyAlarm>, updated: List<DailyAlarm>) {
        previous.distinctBy { it.minutesSinceMidnight }.forEach { cancel(context, it) }
        scheduleAll(context, updated)
    }

    fun cancel(context: Context) {
        // Keep this compatibility method for existing callers: cancel the legacy default slot.
        cancel(context, DailyAlarm(6, 0))
    }

    fun cancelAll(context: Context, alarms: List<DailyAlarm>) {
        alarms.distinctBy { it.minutesSinceMidnight }.forEach { cancel(context, it) }
    }

    fun rescheduleFromSettings(context: Context) {
        kotlinx.coroutines.runBlocking {
            val alarms = SettingsStore(context).alarmTimes.first()
            scheduleAll(context, alarms)
        }
    }

    private fun schedule(context: Context, alarm: DailyAlarm) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val trigger = nextTrigger(alarm.hour, alarm.minute)
        val pendingIntent = pendingIntent(context, alarm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        }
    }

    private fun cancel(context: Context, alarm: DailyAlarm) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, alarm))
    }

    private fun pendingIntent(context: Context, alarm: DailyAlarm): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_BASE + alarm.minutesSinceMidnight,
        Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION)
            .putExtra(EXTRA_HOUR, alarm.hour)
            .putExtra(EXTRA_MINUTE, alarm.minute),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun nextTrigger(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(hour.coerceIn(0, 23)).withMinute(minute.coerceIn(0, 59)).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
