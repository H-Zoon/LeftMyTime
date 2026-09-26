package com.devidea.timeleft.widget

import com.devidea.timeleft.calc.currentOccurrence
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Build
import android.appwidget.AppWidgetProviderInfo
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.net.toUri
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.InterfaceItem
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.repository.TimeLeftRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class AppWidget : AppWidgetProvider() {

    companion object {
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val pickerMutex = Mutex()
        private val renderMutex = Mutex()

        private val providerClasses = listOf(
            AppWidget::class.java,
            MediumAppWidget::class.java,
            WideAppWidget::class.java,
            LargeAppWidget::class.java,
            ScheduleAppWidget::class.java,
            PeriodAppWidget::class.java
        )

        fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
        ) {
            val provider = AppWidget()
            providerClasses.forEach { providerClass ->
                val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, providerClass))
                ids.forEach { appWidgetId ->
                    provider.updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            updatePickerPreviews(context)
        }

        fun updatePickerPreviews(context: Context) {
            if (Build.VERSION.SDK_INT < 35) return
            val appContext = context.applicationContext
            widgetScope.launch {
                pickerMutex.withLock {
                    val renderer = AppWidget()
                    val ep = renderer.entryPoint(appContext)
                    val prefs = ep.prefs()
                    val palette = WidgetPalette.fromPreferences(appContext, prefs)
                    val showProgress = prefs.getString(UserPreferences.KEY_PROGRESS_DISPLAY, UserPreferences.PROGRESS_DISPLAY_FULL) != UserPreferences.PROGRESS_DISPLAY_HIDDEN
                    val generator = ep.itemGenerator()
                    val periods = listOf(generator.timeItem(), generator.monthItem(), generator.yearItem())
                    val manager = AppWidgetManager.getInstance(appContext)
                    val design = com.devidea.timeleft.ui.theme.TimeLeftThemes.TimeFocus
                    val signature = "${design.id}:${design.version}:v2-time-7:${palette}:${appContext.resources.configuration.locales.toLanguageTags()}:${appContext.resources.configuration.fontScale}:$showProgress"
                    val now = System.currentTimeMillis()
                    providerClasses.forEach { provider ->
                        val cacheKey = "widget_picker_${provider.simpleName}"
                        val previous = prefs.getLong("${cacheKey}_updated", 0L)
                        if (prefs.getString(cacheKey, null) == signature && now - previous in 0 until 3_600_000L) return@forEach
                        val source = WidgetSource.defaultForProvider(provider.name)
                        val item = when (source) {
                            WidgetSource.Today -> periods[0]
                            WidgetSource.Month -> periods[1]
                            WidgetSource.Year -> periods[2]
                            WidgetSource.Week -> com.devidea.timeleft.periods.calendarPeriodItem(appContext, com.devidea.timeleft.periods.CalendarPeriod.Week)
                            else -> null
                        }
                        // Calendar values are real; the schedule preview never exposes personal titles.
                        val views = widgetPreviewForSizes(source) { size ->
                            renderer.createViews(appContext, size, WidgetConfiguration(source = source),
                                item, periods, palette, R.string.widget_choose_schedule, showProgress)
                        }
                        val published = runCatching {
                            manager.setWidgetPreview(ComponentName(appContext, provider), AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN, views)
                        }.getOrDefault(false)
                        if (published) prefs.edit { putString(cacheKey, signature); putLong("${cacheKey}_updated", now) }
                    }
                }
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppWidgetEntryPoint {
        fun prefs(): SharedPreferences
        fun telemetry(): com.devidea.timeleft.telemetry.AppTelemetry
        fun itemGenerator(): InterfaceItem
        fun repository(): TimeLeftRepository
    }

    private fun entryPoint(context: Context): AppWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppWidgetEntryPoint::class.java
        )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED)) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            widgetScope.launch {
                try {
                    val manager = AppWidgetManager.getInstance(appContext)
                    providerClasses.forEach { provider ->
                        manager.getAppWidgetIds(ComponentName(appContext, provider)).forEach { renderWidget(appContext, manager, it) }
                    }
                } finally { pendingResult.finish() }
            }
        } else super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        widgetScope.launch {
            try {
                appWidgetIds.forEach { renderWidget(appContext, appWidgetManager, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        widgetScope.launch {
            try {
                renderWidget(appContext, appWidgetManager, appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        context ?: return
        val prefs = entryPoint(context).prefs()
        appWidgetIds?.forEach { id ->
            prefs.edit {
                remove(id.toString())
                remove("${id}option")
                remove(WidgetConfiguration.displayKey(id))
            }
        }
    }

    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        onComplete: () -> Unit = {},
    ) {
        val appContext = context.applicationContext
        widgetScope.launch {
            try { renderWidget(appContext, appWidgetManager, appWidgetId) }
            finally { onComplete() }
        }
    }

    private suspend fun renderWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) = renderMutex.withLock {
        // Serialize slow repository reads so an older render cannot replace a new selection.
        renderWidgetContent(context, appWidgetManager, appWidgetId)
    }

    private suspend fun renderWidgetContent(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        if (appWidgetManager.getAppWidgetInfo(appWidgetId) == null) return
        val ep = entryPoint(context)
        val prefs = ep.prefs()
        val itemGenerator = ep.itemGenerator()
        val repository = ep.repository()

        val configuration = WidgetConfiguration.read(prefs, appWidgetId)
        val periods = listOf(itemGenerator.timeItem(), itemGenerator.monthItem(), itemGenerator.yearItem())
        var emptyMessage = R.string.widget_no_upcoming
        val item = when (configuration.source) {
            WidgetSource.Today -> periods[0]
            WidgetSource.Month -> periods[1]
            WidgetSource.Year -> periods[2]
            WidgetSource.Week -> com.devidea.timeleft.periods.calendarPeriodItem(context, com.devidea.timeleft.periods.CalendarPeriod.Week)
            WidgetSource.Quarter -> com.devidea.timeleft.periods.calendarPeriodItem(context, com.devidea.timeleft.periods.CalendarPeriod.Quarter)
            WidgetSource.Overview -> null
            WidgetSource.Next -> {
                try {
                    val selected = NextCountdownSelector.select(
                        repository.allItems().map { it.currentOccurrence() }, clock = com.devidea.timeleft.focus.readFocusClock(context)
                    )
                    selected?.let {
                        when (it.type) {
                            ItemType.Time -> itemGenerator.customTimeItem(it)
                            ItemType.Date -> itemGenerator.customMonthItem(it)
                        }
                    } ?: periods[1].takeIf { configuration.legacySummary }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    emptyMessage = R.string.widget_load_failed
                    periods[1].takeIf { configuration.legacySummary }
                }
            }
            WidgetSource.Custom -> {
                emptyMessage = R.string.widget_selected_deleted
                try {
                    val entity = repository.allItems().firstOrNull { it.id == configuration.itemId }
                    entity?.let { it.currentOccurrence() }?.let { selected ->
                        when (selected.type) {
                            ItemType.Time -> itemGenerator.customTimeItem(selected)
                            ItemType.Date -> itemGenerator.customMonthItem(selected)
                        }
                    } ?: periods[1].takeIf { configuration.legacySummary }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    emptyMessage = R.string.widget_load_failed
                    periods[1].takeIf { configuration.legacySummary } // Preserve all saved selections on failure.
                }
            }
        }
        val palette = WidgetPalette.fromPreferences(context, prefs)
        val showProgress = prefs.getString(UserPreferences.KEY_PROGRESS_DISPLAY, UserPreferences.PROGRESS_DISPLAY_FULL) != UserPreferences.PROGRESS_DISPLAY_HIDDEN
        val views = widgetViewsForSizes(appWidgetManager.getAppWidgetOptions(appWidgetId), configuration.source) { size ->
            createViews(context, size, configuration, item, periods, palette, emptyMessage, showProgress).also {
                val shownSource = if (configuration.source in listOf(WidgetSource.Custom, WidgetSource.Next) && item?.id == 0)
                    WidgetSource.Month else configuration.source
                bindWidgetActions(context, appWidgetManager, it, appWidgetId, shownSource, item, emptyMessage)
            }
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun bindWidgetActions(
        context: Context,
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetId: Int,
        source: WidgetSource,
        item: AdapterItem?,
        emptyMessage: Int,
    ) {
        val provider = appWidgetManager.getAppWidgetInfo(appWidgetId)?.provider
            ?: ComponentName(context, AppWidget::class.java)
        val updateIntent = Intent().apply {
            component = provider
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }

        val updatePendingIntent =
            PendingIntent.getBroadcast(
                context,
                appWidgetId,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        fun details(period: WidgetSource, id: Int? = null): PendingIntent = PendingIntent.getActivity(
            context, appWidgetId, WidgetDetailsActivity.createIntent(context, appWidgetId, period, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val configureIntent = Intent(context, AppWidgetConfigure::class.java).apply {
            data = "timeleft://widget/$appWidgetId/configure".toUri()
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val configureAction = PendingIntent.getActivity(context, appWidgetId, configureIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val rootAction = when {
            views.layoutId == R.layout.app_widget_resize -> configureAction
            item == null && emptyMessage == R.string.widget_load_failed -> updatePendingIntent
            item == null -> configureAction
            else -> details(source, item.id.takeIf { it > 0 })
        }
        if (source == WidgetSource.Overview && views.layoutId != R.layout.app_widget_resize) {
            listOf(R.id.overviewToday to WidgetSource.Today, R.id.overviewMonth to WidgetSource.Month, R.id.overviewYear to WidgetSource.Year)
                .forEach { (viewId, period) -> views.setOnClickPendingIntent(viewId, details(period)) }
        } else views.setOnClickPendingIntent(R.id.widgetRoot, rootAction)
        if (views.layoutId == R.layout.app_widget_single || views.layoutId == R.layout.app_widget_board) {
            views.setOnClickPendingIntent(R.id.refresh, updatePendingIntent)
            views.setOnClickPendingIntent(R.id.percent, rootAction)
        }
    }

    /** Shared by configuration, launcher previews, debug gallery and installed widgets. */
    private fun createViews(
        context: Context,
        dimensions: WidgetDimensions,
        configuration: WidgetConfiguration,
        item: AdapterItem?,
        periods: List<AdapterItem>,
        palette: WidgetPalette,
        emptyMessage: Int,
        showProgress: Boolean = true,
        snapshotTimeMillis: Long? = null,
    ): RemoteViews = if (configuration.source == WidgetSource.Overview) {
        createOverviewViews(context, dimensions, periods, palette)
    } else {
        createSingleWidgetViews(context, dimensions, configuration, item, periods, palette, emptyMessage, showProgress, snapshotTimeMillis)
    }

    internal fun previewViews(
        context: Context,
        dimensions: WidgetDimensions,
        configuration: WidgetConfiguration,
        item: AdapterItem?,
        periods: List<AdapterItem>,
        paletteKey: String,
        dark: Boolean,
        emptyMessage: Int = R.string.widget_no_upcoming,
        showProgress: Boolean = true,
        snapshotTimeMillis: Long? = null,
        designKey: String = UserPreferences.DESIGN_TIME_FOCUS,
    ): RemoteViews = createViews(
        context, dimensions, configuration, item, periods,
        WidgetPalette.create(paletteKey, dark, designKey), emptyMessage, showProgress, snapshotTimeMillis,
    )

}

class MediumAppWidget : AppWidget()
class WideAppWidget : AppWidget()
class LargeAppWidget : AppWidget()
class ScheduleAppWidget : AppWidget()
class PeriodAppWidget : AppWidget()
