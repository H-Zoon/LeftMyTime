package com.devidea.timeleft.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devidea.timeleft.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val items = AppDatabase.getDatabase(context.applicationContext)
                    .itemDao()
                    .getItems()
                ReminderScheduler.rescheduleAll(context.applicationContext, items)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
