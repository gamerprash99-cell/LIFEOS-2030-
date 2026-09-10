package com.lifeos.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.lifeos.app.core.di.ServiceLocator

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_TIME_CHANGED &&
            intent?.action != Intent.ACTION_TIMEZONE_CHANGED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val store = ServiceLocator.get(context.applicationContext).settingsStore
                if (store.alarmEnabled.first()) {
                    AlarmScheduler.scheduleDaily(context.applicationContext, store.alarmHour.first(), store.alarmMinute.first())
                }
            } finally {
                pending.finish()
            }
        }
    }
}
