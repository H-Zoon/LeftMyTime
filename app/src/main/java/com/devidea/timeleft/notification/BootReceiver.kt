package com.devidea.timeleft.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devidea.timeleft.repository.TimeLeftRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TimeLeftRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val items = repository.allItems()
                ReminderScheduler.rescheduleAll(appContext, items)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
