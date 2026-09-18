package com.devidea.timeleft.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import com.devidea.timeleft.ui.theme.ThemePalette
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.InterfaceItem
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.MainActivity
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

open class AppWidget : AppWidgetProvider() {

    companion object {
        private const val ACTION_OPEN_FROM_WIDGET =
            "com.devidea.timeleft.action.OPEN_FROM_WIDGET"
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val providerClasses = listOf(
            AppWidget::class.java,
            MediumAppWidget::class.java,
            WideAppWidget::class.java,
            LargeAppWidget::class.java,
            ScheduleAppWidget::class.java
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
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppWidgetEntryPoint {
        fun prefs(): SharedPreferences
        fun itemGenerator(): InterfaceItem
        fun repository(): TimeLeftRepository
    }

    private fun entryPoint(context: Context): AppWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppWidgetEntryPoint::class.java
        )

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
    ) {
        val appContext = context.applicationContext
        widgetScope.launch {
            renderWidget(appContext, appWidgetManager, appWidgetId)
        }
    }

    private suspend fun renderWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val ep = entryPoint(context)
        val prefs = ep.prefs()
        val itemGenerator = ep.itemGenerator()
        val repository = ep.repository()

        val configuration = WidgetConfiguration.read(prefs, appWidgetId)
        val dimensions = WidgetDimensions.fromOptions(appWidgetManager.getAppWidgetOptions(appWidgetId))
        val periods = listOf(itemGenerator.timeItem(), itemGenerator.monthItem(), itemGenerator.yearItem())
        var emptyMessage = R.string.widget_no_upcoming
        val item = when (configuration.source) {
            WidgetSource.Today -> periods[0]
            WidgetSource.Month -> periods[1]
            WidgetSource.Year -> periods[2]
            WidgetSource.Overview -> null
            WidgetSource.Next -> {
                try {
                    val selected = NextCountdownSelector.select(
                        repository.advanceExpiredRecurrences(repository.allItems())
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
                    emptyMessage = R.string.widget_item_unavailable
                    periods[1].takeIf { configuration.legacySummary }
                }
            }
            WidgetSource.Custom -> {
                emptyMessage = R.string.widget_item_unavailable
                try {
                    val itemId = configuration.itemId ?: error("Missing widget item")
                    val selected = repository.advanceExpiredRecurrence(repository.getItem(itemId))
                    when (selected.type) {
                        ItemType.Time -> itemGenerator.customTimeItem(selected)
                        ItemType.Date -> itemGenerator.customMonthItem(selected)
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    if (configuration.legacySummary) {
                        prefs.edit { remove(appWidgetId.toString()) }
                        periods[1]
                    } else null // Keep the ID so a transient failure does not erase the user's choice.
                }
            }
        }
        val views = createViews(
            context, dimensions, configuration, item, periods,
            WidgetPalette.fromPreferences(context, prefs), emptyMessage,
        )
        bindWidgetActions(context, appWidgetManager, views, appWidgetId, dimensions.sizeClass, configuration.source)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun bindWidgetActions(
        context: Context,
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetId: Int,
        sizeClass: WidgetSizeClass,
        source: WidgetSource,
    ) {
        val provider = appWidgetManager.getAppWidgetInfo(appWidgetId)?.provider
            ?: ComponentName(context, AppWidget::class.java)
        val updateIntent = Intent().apply {
            component = provider
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }

        val updatePendingIntent =
            PendingIntent.getBroadcast(
                context,
                appWidgetId,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_FROM_WIDGET
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (source != WidgetSource.Overview && (sizeClass == WidgetSizeClass.Medium || sizeClass == WidgetSizeClass.Large)) {
            views.setOnClickPendingIntent(R.id.refresh, updatePendingIntent)
        }
        views.setOnClickPendingIntent(R.id.widgetRoot, activityPendingIntent)
        if (source != WidgetSource.Overview) views.setOnClickPendingIntent(R.id.percent, activityPendingIntent)
    }

    private fun applyPalette(
        context: Context,
        views: RemoteViews,
        sizeClass: WidgetSizeClass,
        palette: WidgetPalette,
    ) {
        views.setInt(R.id.widgetRoot, "setBackgroundResource", palette.backgroundDrawableRes)
        views.setTextColor(R.id.summary, palette.onSurface)
        views.setTextColor(R.id.percent, palette.onSurface)
        if (sizeClass == WidgetSizeClass.Medium || sizeClass == WidgetSizeClass.Large) {
            views.setInt(R.id.refresh, "setColorFilter", palette.muted)
        }
        if (sizeClass != WidgetSizeClass.Compact) views.setTextColor(R.id.widgetMeta, palette.muted)
        if (sizeClass == WidgetSizeClass.Large) {
            listOf(R.id.widgetFlowToday, R.id.widgetFlowMonth, R.id.widgetFlowYear).forEach { id ->
                views.setTextColor(id, palette.muted)
            }
        }
    }

    /** Shared by configuration previews, the debug gallery, and installed widgets. */
    private fun createViews(
        context: Context,
        dimensions: WidgetDimensions,
        configuration: WidgetConfiguration,
        item: AdapterItem?,
        periods: List<AdapterItem>,
        palette: WidgetPalette,
        emptyMessage: Int,
    ): RemoteViews {
        if (configuration.source == WidgetSource.Overview) {
            return createOverviewViews(context, dimensions, periods, palette.backgroundDrawableRes, palette.onSurface, palette.muted)
        }
        val size = dimensions.sizeClass
        val views = RemoteViews(context.packageName, size.layoutRes)
        applyPalette(context, views, size, palette)
        val data = item?.toWidgetData(
            context, configuration.showRemaining,
            configuration.source == WidgetSource.Today || item.type == ItemType.Time,
        ) ?: WidgetDisplayData(
            title = context.getString(configuration.source.labelRes),
            value = context.getString(emptyMessage),
            meta = "", progress = 0,
            accessibilityText = context.getString(configuration.source.labelRes) + ", " + context.getString(emptyMessage),
        )
        renderBase(views, data, palette, size)
        if (item == null) {
            views.setTextViewTextSize(R.id.percent, android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            views.setInt(R.id.percent, "setMaxLines", Int.MAX_VALUE)
        }
        if (size != WidgetSizeClass.Compact) renderMedium(views, data)
        if (size == WidgetSizeClass.Large && configuration.legacySummary) {
            renderLarge(views, data, WidgetFlowItems(periods[0], periods[1], periods[2]))
        }
        adaptToHeight(views, size, dimensions.height, palette.fontScale, configuration.legacySummary)
        if (item == null && (size == WidgetSizeClass.Medium || size == WidgetSizeClass.Large)) {
            views.setViewVisibility(R.id.widgetRuler, View.GONE)
        }
        return views
    }

    private fun renderBase(
        views: RemoteViews,
        data: WidgetDisplayData,
        palette: WidgetPalette,
        sizeClass: WidgetSizeClass,
    ) {
        views.setTextViewText(R.id.summary, data.title)
        views.setTextViewText(R.id.percent, data.value)
        // Hosts may reapply the same layout after an empty state; restore the XML's value typography.
        val valueSize = when (sizeClass) {
            WidgetSizeClass.Compact -> if (data.value.length >= 7) 16f else 22f
            WidgetSizeClass.Medium -> 32f
            WidgetSizeClass.Wide -> 28f
            WidgetSizeClass.Large -> 36f
        }
        views.setTextViewTextSize(R.id.percent, android.util.TypedValue.COMPLEX_UNIT_SP, valueSize)
        views.setInt(R.id.percent, "setMaxLines", 2)
        views.setContentDescription(R.id.percent, data.accessibilityText)
        if (sizeClass == WidgetSizeClass.Medium || sizeClass == WidgetSizeClass.Large) {
            views.setImageViewBitmap(R.id.widgetRuler, rulerBitmap(data.progress, palette))
        }
    }

    private fun renderMedium(
        views: RemoteViews,
        data: WidgetDisplayData,
    ) {
        views.setTextViewText(R.id.widgetMeta, data.meta)
    }

    private fun renderLarge(
        views: RemoteViews,
        data: WidgetDisplayData,
        flowItems: WidgetFlowItems,
    ) {
        renderMedium(views, data)
        setFlowText(views, R.id.widgetFlowToday, flowItems.today)
        setFlowText(views, R.id.widgetFlowMonth, flowItems.month)
        setFlowText(views, R.id.widgetFlowYear, flowItems.year)
    }

    private fun setFlowText(
        views: RemoteViews,
        viewId: Int,
        item: AdapterItem,
    ) {
        val value = item.widgetString.ifBlank { item.leftString }
        views.setTextViewText(viewId, "${item.title} - $value")
    }

    private fun rulerBitmap(elapsed: Int, palette: WidgetPalette): Bitmap {
        val bitmap = Bitmap.createBitmap(600, 48, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f }
        repeat(60) { index ->
            paint.color = if ((index + .5f) / 60 < elapsed / 100f) palette.track else palette.primary
            val x = index * 10f + 5f
            canvas.drawLine(x, 48f, x, if (index % 5 == 0) 0f else 24f, paint)
        }
        return bitmap
    }

    /** Small widgets keep the title and value; optional detail appears as space allows. */
    private fun adaptToHeight(views: RemoteViews, size: WidgetSizeClass, height: Int, fontScale: Float, legacySummary: Boolean) {
        if (size == WidgetSizeClass.Medium || size == WidgetSizeClass.Large) {
            views.setViewVisibility(R.id.refresh, if (height >= 180 * fontScale) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetRuler, if (height >= (if (size == WidgetSizeClass.Large && legacySummary) 260 else 180) * fontScale) View.VISIBLE else View.GONE)
        }
        if (size == WidgetSizeClass.Large) {
            views.setViewVisibility(R.id.widgetFlowToday, if (legacySummary) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetFlowMonth, if (legacySummary && height >= 210 * fontScale) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widgetFlowYear, if (legacySummary && height >= 240 * fontScale) View.VISIBLE else View.GONE)
        }
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
    ): RemoteViews = createViews(
        context, dimensions, configuration, item, periods,
        WidgetPalette.create(context, paletteKey, dark), emptyMessage,
    )

    private data class WidgetPalette(
        val backgroundDrawableRes: Int,
        val primary: Int,
        val onSurface: Int,
        val muted: Int,
        val track: Int,
        val fontScale: Float,
    ) {
        companion object {
            fun fromPreferences(context: Context, prefs: SharedPreferences): WidgetPalette {
                val dark = when (prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO)) {
                    UserPreferences.THEME_DARK -> true
                    UserPreferences.THEME_LIGHT -> false
                    else -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                }
                return create(context, prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY) ?: UserPreferences.COLOR_THEME_CLAY, dark)
            }
            fun create(context: Context, key: String, dark: Boolean): WidgetPalette {
                val palette = ThemePalette.fromKey(key)
                val colors = if (dark) palette.darkColors else palette.lightColors
                return WidgetPalette(
                    if (dark) R.drawable.widget_background_dark else R.drawable.widget_background_light,
                    colors.primary.toArgb(), colors.onBackground.toArgb(), colors.onSurfaceVariant.toArgb(), colors.outlineVariant.toArgb(),
                    context.resources.configuration.fontScale
                )
            }
        }
    }

    private data class WidgetFlowItems(
        val today: AdapterItem,
        val month: AdapterItem,
        val year: AdapterItem,
    )
}

class MediumAppWidget : AppWidget()

class WideAppWidget : AppWidget()

class LargeAppWidget : AppWidget()

class ScheduleAppWidget : AppWidget()
