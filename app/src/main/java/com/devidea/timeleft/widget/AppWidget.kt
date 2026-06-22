package com.devidea.timeleft.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.InterfaceItem
import com.devidea.timeleft.R
import com.devidea.timeleft.formatPercent
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.database.itemdata.ItemEntity
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
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val providerClasses = listOf(
            AppWidget::class.java,
            MediumAppWidget::class.java,
            WideAppWidget::class.java,
            LargeAppWidget::class.java
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

        val sizeClass = resolveSizeClass(appWidgetManager, appWidgetId)
        val views = RemoteViews(context.packageName, sizeClass.layoutRes)
        val palette = WidgetPalette.fromKey(
            prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_INDIGO)
                ?: UserPreferences.COLOR_THEME_INDIGO
        )
        val source = prefs.getString(appWidgetId.toString(), "").orEmpty()
        val showRemaining = prefs.getBoolean("${appWidgetId}option", false)

        val flowItems = WidgetFlowItems(
            today = itemGenerator.timeItem(),
            month = itemGenerator.monthItem(),
            year = itemGenerator.yearItem()
        )

        applyPalette(context, views, sizeClass, palette)
        bindWidgetActions(context, appWidgetManager, views, appWidgetId, sizeClass)

        when (source) {
            "embedYear" -> renderWidgetItem(
                views = views,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
                sizeClass = sizeClass,
                palette = palette,
                data = itemGenerator.yearItem().toWidgetData(
                    context = context,
                    showRemaining = showRemaining,
                    useWidgetString = false
                ),
                flowItems = flowItems
            )
            "embedMonth" -> renderWidgetItem(
                views = views,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
                sizeClass = sizeClass,
                palette = palette,
                data = itemGenerator.monthItem().toWidgetData(
                    context = context,
                    showRemaining = showRemaining,
                    useWidgetString = false
                ),
                flowItems = flowItems
            )
            "embedTime" -> renderWidgetItem(
                views = views,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
                sizeClass = sizeClass,
                palette = palette,
                data = itemGenerator.timeItem().toWidgetData(
                    context = context,
                    showRemaining = showRemaining,
                    useWidgetString = true
                ),
                flowItems = flowItems
            )
            "nextCustom" -> renderNextCountdownWidget(
                context = context,
                views = views,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
                sizeClass = sizeClass,
                palette = palette,
                flowItems = flowItems,
                showRemaining = showRemaining,
                itemGenerator = itemGenerator,
                repository = repository
            )
            else -> renderCustomWidget(
                context = context,
                views = views,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
                sizeClass = sizeClass,
                palette = palette,
                flowItems = flowItems,
                showRemaining = showRemaining,
                source = source,
                prefs = prefs,
                itemGenerator = itemGenerator,
                repository = repository
            )
        }
    }

    private fun bindWidgetActions(
        context: Context,
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetId: Int,
        sizeClass: WidgetSizeClass,
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

        val activityPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (sizeClass == WidgetSizeClass.Medium || sizeClass == WidgetSizeClass.Large) {
            views.setOnClickPendingIntent(R.id.refresh, updatePendingIntent)
        }
        views.setOnClickPendingIntent(R.id.widgetRoot, activityPendingIntent)
        views.setOnClickPendingIntent(R.id.percent, activityPendingIntent)
    }

    private fun applyPalette(
        context: Context,
        views: RemoteViews,
        sizeClass: WidgetSizeClass,
        palette: WidgetPalette,
    ) {
        val primary = ContextCompat.getColor(context, palette.primaryColorRes)
        val onSurface = ContextCompat.getColor(context, palette.onSurfaceColorRes)
        val onSurfaceVariant = ContextCompat.getColor(context, palette.onSurfaceVariantColorRes)

        views.setInt(R.id.widgetRoot, "setBackgroundResource", palette.backgroundDrawableRes)
        if (sizeClass == WidgetSizeClass.Medium || sizeClass == WidgetSizeClass.Large) {
            views.setInt(R.id.refresh, "setBackgroundResource", palette.iconBackgroundDrawableRes)
            views.setInt(R.id.refresh, "setColorFilter", primary)
        }
        views.setTextColor(R.id.summary, primary)
        views.setTextColor(R.id.percent, onSurface)
        WidgetPalette.entries.forEach { widgetPalette ->
            views.setViewVisibility(
                widgetPalette.progressViewId,
                if (widgetPalette == palette) View.VISIBLE else View.GONE
            )
        }

        if (sizeClass != WidgetSizeClass.Compact) {
            views.setTextColor(R.id.widgetMeta, onSurfaceVariant)
        }
        if (sizeClass == WidgetSizeClass.Large) {
            listOf(R.id.widgetFlowToday, R.id.widgetFlowMonth, R.id.widgetFlowYear).forEach { id ->
                views.setInt(id, "setBackgroundResource", palette.iconBackgroundDrawableRes)
                views.setTextColor(id, onSurface)
            }
        }
    }

    private suspend fun renderNextCountdownWidget(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        sizeClass: WidgetSizeClass,
        palette: WidgetPalette,
        flowItems: WidgetFlowItems,
        showRemaining: Boolean,
        itemGenerator: InterfaceItem,
        repository: TimeLeftRepository,
    ) {
        val selected = NextCountdownSelector.select(
            repository.advanceExpiredRecurrences(repository.allItems())
        )

        if (selected == null) {
            renderMonthFallback(
                context = context,
                views = views,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
                sizeClass = sizeClass,
                palette = palette,
                flowItems = flowItems,
                showRemaining = showRemaining,
                itemGenerator = itemGenerator
            )
            return
        }

        val item = selected.toAdapterItem(itemGenerator)
        renderWidgetItem(
            views = views,
            appWidgetManager = appWidgetManager,
            appWidgetId = appWidgetId,
            sizeClass = sizeClass,
            palette = palette,
            data = item.toWidgetData(
                context = context,
                showRemaining = showRemaining,
                useWidgetString = selected.type == ItemType.Time
            ),
            flowItems = flowItems
        )
    }

    private suspend fun renderCustomWidget(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        sizeClass: WidgetSizeClass,
        palette: WidgetPalette,
        flowItems: WidgetFlowItems,
        showRemaining: Boolean,
        source: String,
        prefs: SharedPreferences,
        itemGenerator: InterfaceItem,
        repository: TimeLeftRepository,
    ) {
        val selectedItemId = source.toIntOrNull()
        if (selectedItemId == null) {
            clearWidgetSelection(prefs, appWidgetId)
            renderMonthFallback(
                context, views, appWidgetManager, appWidgetId, sizeClass, palette,
                flowItems, showRemaining, itemGenerator
            )
            return
        }

        try {
            val itemEntity = repository.advanceExpiredRecurrence(
                repository.getItem(selectedItemId)
            )
            val item = itemEntity.toAdapterItem(itemGenerator)

            renderWidgetItem(
                views = views,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
                sizeClass = sizeClass,
                palette = palette,
                data = item.toWidgetData(
                    context = context,
                    showRemaining = showRemaining,
                    useWidgetString = itemEntity.type == ItemType.Time
                ),
                flowItems = flowItems
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            clearWidgetSelection(prefs, appWidgetId)
            renderMonthFallback(
                context, views, appWidgetManager, appWidgetId, sizeClass, palette,
                flowItems, showRemaining, itemGenerator
            )
        }
    }

    private fun ItemEntity.toAdapterItem(itemGenerator: InterfaceItem): AdapterItem =
        when (type) {
            ItemType.Time -> itemGenerator.customTimeItem(this)
            ItemType.Date -> itemGenerator.customMonthItem(this)
        }

    private fun clearWidgetSelection(prefs: SharedPreferences, appWidgetId: Int) {
        prefs.edit { remove(appWidgetId.toString()) }
    }

    private fun renderMonthFallback(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        sizeClass: WidgetSizeClass,
        palette: WidgetPalette,
        flowItems: WidgetFlowItems,
        showRemaining: Boolean,
        itemGenerator: InterfaceItem,
    ) {
        renderWidgetItem(
            views = views,
            appWidgetManager = appWidgetManager,
            appWidgetId = appWidgetId,
            sizeClass = sizeClass,
            palette = palette,
            data = itemGenerator.monthItem().toWidgetData(
                context = context,
                showRemaining = showRemaining,
                useWidgetString = false
            ),
            flowItems = flowItems
        )
    }

    private fun renderWidgetItem(
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        sizeClass: WidgetSizeClass,
        palette: WidgetPalette,
        data: WidgetDisplayData,
        flowItems: WidgetFlowItems,
    ) {
        renderBase(views, data, palette)
        when (sizeClass) {
            WidgetSizeClass.Compact -> Unit
            WidgetSizeClass.Medium -> renderMedium(views, data)
            WidgetSizeClass.Wide -> renderMedium(views, data)
            WidgetSizeClass.Large -> renderLarge(views, data, flowItems)
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun renderBase(
        views: RemoteViews,
        data: WidgetDisplayData,
        palette: WidgetPalette,
    ) {
        views.setTextViewText(R.id.summary, data.title)
        views.setTextViewText(R.id.percent, data.value)
        views.setProgressBar(palette.progressViewId, 100, data.progress, false)
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

    private fun AdapterItem.toWidgetData(
        context: Context,
        showRemaining: Boolean,
        useWidgetString: Boolean,
    ): WidgetDisplayData {
        val remainingText = if (useWidgetString && widgetString.isNotBlank()) {
            widgetString
        } else {
            leftString
        }
        val value = if (showRemaining) {
            remainingText
        } else {
            countdownText.ifBlank { remainingText }
        }
        val meta = when {
            dueText.isNotBlank() -> dueText
            else -> context.getString(R.string.card_progress_value, formatPercent(percent))
        }

        return WidgetDisplayData(
            title = title,
            value = value,
            meta = meta,
            progress = percent.toInt().coerceIn(0, 100),
        )
    }

    private fun resolveSizeClass(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ): WidgetSizeClass {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        return when {
            minWidth >= 245 && minHeight >= 185 -> WidgetSizeClass.Large
            minWidth >= 245 && minHeight < 115 -> WidgetSizeClass.Wide
            minHeight >= 115 -> WidgetSizeClass.Medium
            else -> WidgetSizeClass.Compact
        }
    }

    private enum class WidgetSizeClass(val layoutRes: Int) {
        Compact(R.layout.app_widget),
        Medium(R.layout.app_widget_medium),
        Wide(R.layout.app_widget_wide),
        Large(R.layout.app_widget_large)
    }

    private enum class WidgetPalette(
        val key: String,
        val backgroundDrawableRes: Int,
        val iconBackgroundDrawableRes: Int,
        val progressViewId: Int,
        val primaryColorRes: Int,
        val onSurfaceColorRes: Int,
        val onSurfaceVariantColorRes: Int,
    ) {
        Indigo(
            UserPreferences.COLOR_THEME_INDIGO,
            R.drawable.line_widget,
            R.drawable.widget_icon_button,
            R.id.progress,
            R.color.widget_primary,
            R.color.widget_on_surface,
            R.color.widget_on_surface_variant
        ),
        Emerald(
            UserPreferences.COLOR_THEME_EMERALD,
            R.drawable.line_widget_emerald,
            R.drawable.widget_icon_button_emerald,
            R.id.progressEmerald,
            R.color.widget_primary_emerald,
            R.color.widget_on_surface_emerald,
            R.color.widget_on_surface_variant_emerald
        ),
        Rose(
            UserPreferences.COLOR_THEME_ROSE,
            R.drawable.line_widget_rose,
            R.drawable.widget_icon_button_rose,
            R.id.progressRose,
            R.color.widget_primary_rose,
            R.color.widget_on_surface_rose,
            R.color.widget_on_surface_variant_rose
        ),
        Amber(
            UserPreferences.COLOR_THEME_AMBER,
            R.drawable.line_widget_amber,
            R.drawable.widget_icon_button_amber,
            R.id.progressAmber,
            R.color.widget_primary_amber,
            R.color.widget_on_surface_amber,
            R.color.widget_on_surface_variant_amber
        ),
        Slate(
            UserPreferences.COLOR_THEME_SLATE,
            R.drawable.line_widget_slate,
            R.drawable.widget_icon_button_slate,
            R.id.progressSlate,
            R.color.widget_primary_slate,
            R.color.widget_on_surface_slate,
            R.color.widget_on_surface_variant_slate
        );

        companion object {
            fun fromKey(key: String): WidgetPalette =
                entries.firstOrNull { it.key == key } ?: Indigo
        }
    }

    private data class WidgetDisplayData(
        val title: String,
        val value: String,
        val meta: String,
        val progress: Int,
    )

    private data class WidgetFlowItems(
        val today: AdapterItem,
        val month: AdapterItem,
        val year: AdapterItem,
    )
}

class MediumAppWidget : AppWidget()

class WideAppWidget : AppWidget()

class LargeAppWidget : AppWidget()
