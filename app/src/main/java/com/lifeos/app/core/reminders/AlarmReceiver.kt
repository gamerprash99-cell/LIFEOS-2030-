package com.lifeos.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifeos.app.core.di.ServiceLocator
import com.lifeos.app.core.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val store = ServiceLocator.get(appContext).settingsStore
                val alarms = store.alarmTimes.first()
                val hour = intent?.getIntExtra("extra_hour", -1) ?: -1
                val minute = intent?.getIntExtra("extra_minute", -1) ?: -1
                // The missing extras case is kept for alarms created by older builds.
                val alarm = if (hour >= 0 && minute >= 0) {
                    alarms.firstOrNull { it.hour == hour && it.minute == minute }
                } else {
                    alarms.firstOrNull()
                }
                if (alarm != null) {
                    NotificationHelper.showAlarmNotification(appContext)
                    AlarmScheduler.scheduleDaily(appContext, alarm.hour, alarm.minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
