package com.devidea.timeleft.focus

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.devidea.timeleft.widget.AppWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FocusReceiver : BroadcastReceiver() {
    @Inject lateinit var focus: FocusCoordinator
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { focus.refresh(); AppWidget.updateAllWidgets(context, AppWidgetManager.getInstance(context)) }
            catch (error: Exception) { Log.e("FocusReceiver", "Focus refresh failed: ${error.javaClass.simpleName}") }
            finally { pending.finish() }
        }
    }
}
