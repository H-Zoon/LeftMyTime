package com.devidea.timeleft.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.theme.LayoutTokens
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Debug galleries use fixture IDs and must not create widgets for real stored items. */
internal val LocalWidgetPinAllowed = staticCompositionLocalOf { true }

/** Kept in expanded details so widget promotion does not compete with the remaining time. */
@Composable
internal fun PinWidgetButton(item: AdapterItem, source: WidgetSource, enabled: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var requesting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<Int?>(null) }
    TextButton(
        onClick = {
            requesting = true
            scope.launch {
                try { errorMessage = requestWidgetPin(context, source, item.id) }
                finally { requesting = false }
            }
        },
        enabled = enabled && LocalWidgetPinAllowed.current && !requesting,
        modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget),
    ) { Text(stringResource(R.string.widget_pin_action)) }
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.widget_pin_action)) },
            text = { Text(stringResource(message)) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text(stringResource(R.string.action_confirm)) } },
        )
    }
}

private fun WidgetSource.providerClass(): Class<out AppWidget> = when (this) {
    WidgetSource.Today -> AppWidget::class.java
    WidgetSource.Month -> MediumAppWidget::class.java
    WidgetSource.Year -> WideAppWidget::class.java
    WidgetSource.Overview -> LargeAppWidget::class.java
    WidgetSource.Next, WidgetSource.Custom -> ScheduleAppWidget::class.java
}

/** A null result means the launcher accepted the request, not that the user added the widget. */
private suspend fun requestWidgetPin(context: Context, source: WidgetSource, itemId: Int): Int? {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return R.string.widget_pin_unsupported
    try {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, AppWidget.AppWidgetEntryPoint::class.java)
        val generator = ep.itemGenerator()
        // An automatic widget's detail pins the item the user is actually viewing.
        val pinnedSource = if (source == WidgetSource.Next) WidgetSource.Custom else source
        val configuration = WidgetConfiguration(pinnedSource, itemId.takeIf { pinnedSource == WidgetSource.Custom })
        val item = when (pinnedSource) {
            WidgetSource.Today -> generator.timeItem()
            WidgetSource.Month -> generator.monthItem()
            WidgetSource.Year -> generator.yearItem()
            WidgetSource.Custom -> {
                val entity = withContext(Dispatchers.IO) { ep.repository().allItems().firstOrNull { it.id == itemId } }
                    ?.forWidgetPreview() ?: return R.string.widget_selected_deleted
                if (entity.type == ItemType.Time) generator.customTimeItem(entity) else generator.customMonthItem(entity)
            }
            else -> null
        }
        val prefs = ep.prefs()
        val palette = WidgetPalette.fromPreferences(context, prefs)
        val periods = listOf(generator.timeItem(), generator.monthItem(), generator.yearItem())
        val preview = widgetPreviewForSizes(pinnedSource) { size ->
            if (pinnedSource == WidgetSource.Overview) createOverviewViews(context, size, periods, palette)
            else createSingleWidgetViews(context, size, configuration, item, periods, palette, R.string.widget_no_upcoming,
                prefs.getString(UserPreferences.KEY_PROGRESS_DISPLAY, UserPreferences.PROGRESS_DISPLAY_FULL) != UserPreferences.PROGRESS_DISPLAY_HIDDEN)
        }
        // Fixed URI carries the selection; only EXTRA_APPWIDGET_ID is supplied by the launcher.
        // Each request has its own one-shot callback, including repeated pins of the same item.
        val callbackIntent = Intent(context, WidgetPinReceiver::class.java).apply {
            data = Uri.Builder().scheme("timeleft").authority("pin").appendPath(UUID.randomUUID().toString())
                .appendPath(pinnedSource.prefValue).appendPath(configuration.itemId?.toString() ?: "period").build()
        }
        // Mutable is required for the system to append the new widget ID; the receiver is explicit and private.
        val flags = PendingIntent.FLAG_ONE_SHOT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val callback = PendingIntent.getBroadcast(context, 0, callbackIntent, flags)
        val accepted = try {
            manager.requestPinAppWidget(ComponentName(context, pinnedSource.providerClass()),
                Bundle().apply { putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview) }, callback)
        } catch (exception: Exception) {
            callback.cancel()
            throw exception
        }
        if (!accepted) callback.cancel()
        return if (accepted) null else R.string.widget_pin_unsupported
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        return R.string.widget_pin_failed
    }
}

/** Only the successful launcher callback persists a selection, so cancellation leaves no saved widget. */
class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme != "timeleft" || data.authority != "pin" || data.pathSegments.size != 3) return
        val source = WidgetSource.entries.firstOrNull { it.prefValue == data.pathSegments[1] } ?: return
        if (source == WidgetSource.Next) return
        val itemId = data.pathSegments[2].toIntOrNull()
        if (source == WidgetSource.Custom && (itemId == null || itemId <= 0)) return
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val manager = AppWidgetManager.getInstance(context)
        if (manager.getAppWidgetInfo(id)?.provider != ComponentName(context, source.providerClass())) return
        val prefs = EntryPointAccessors.fromApplication(context.applicationContext, AppWidget.AppWidgetEntryPoint::class.java).prefs()
        if (WidgetConfiguration.hasSavedSettings(prefs, id)) return
        WidgetConfiguration(source, itemId).write(prefs, id)
        val pending = goAsync()
        AppWidget().updateAppWidget(context, manager, id, onComplete = pending::finish)
    }
}
