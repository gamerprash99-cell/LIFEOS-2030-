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
                if (store.alarmEnabled.first()) {
                    NotificationHelper.showAlarmNotification(appContext)
                    AlarmScheduler.scheduleDaily(appContext, store.alarmHour.first(), store.alarmMinute.first())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
