package com.devidea.timeleft.design

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.devidea.timeleft.ui.components.TimeLeftDatePicker
import com.devidea.timeleft.ui.components.TimeLeftTimePicker
import com.devidea.timeleft.widget.AppWidget
import com.devidea.timeleft.widget.WidgetConfiguration
import com.devidea.timeleft.widget.WidgetDimensions
import com.devidea.timeleft.widget.WidgetSource
import com.devidea.timeleft.widget.WidgetConfigureRoute
import com.devidea.timeleft.widget.WidgetSaveResult
import com.devidea.timeleft.widget.LocalWidgetPinAllowed
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.TimeDetailFacts
import com.devidea.timeleft.TimeDetailRange
import com.devidea.timeleft.ui.components.TimeDetailContent
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.ItemVisuals
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.editor.ItemEditorScreen
import com.devidea.timeleft.ui.home.HomeScreen
import com.devidea.timeleft.ui.settings.SettingsScreen
import com.devidea.timeleft.ui.settings.ThemePreviewScreen
import com.devidea.timeleft.ui.settings.ThemePreviewSnapshot
import com.devidea.timeleft.ui.settings.ThemePreviewState
import com.devidea.timeleft.ui.theme.ThemePalette
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import com.devidea.timeleft.ui.theme.systemUsesDarkTheme
import java.util.Locale

/** Opens real screen components with local fixtures; never writes user data. */
open class DesignGalleryActivity : ComponentActivity() {
    protected open val galleryLocale: Locale get() = Locale.KOREAN

    // This gallery exists only in debug APKs, which include every locale.
    @SuppressLint("AppBundleLocaleChanges")
    override fun attachBaseContext(newBase: Context) {
        val configuration = android.content.res.Configuration(newBase.resources.configuration)
        configuration.setLocale(galleryLocale)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "home"
        val palette = intent.getStringExtra("palette") ?: UserPreferences.COLOR_THEME_CLAY
        val mode = intent.getStringExtra("theme") ?: UserPreferences.THEME_LIGHT
        val design = intent.getStringExtra("design") ?: UserPreferences.DESIGN_TIME_FOCUS
        val fontScale = intent.getFloatExtra("fontScale", 1f).coerceIn(1f, 2f)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = mode != UserPreferences.THEME_DARK
            isAppearanceLightNavigationBars = mode != UserPreferences.THEME_DARK
        }
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale), LocalWidgetPinAllowed provides false) {
                TimeLeftTheme(themeMode = mode, paletteKey = palette, designKey = design) {
                    GalleryScreen(screen, palette, mode, intent.getBooleanExtra("exportWidget", false))
                }
            }
        }
    }
}

private val sampleItems = listOf(
    AdapterItem(id = 4, title = "저녁 독서", type = ItemType.Time, timePhase = TimeRangePhase.Upcoming, secondsUntilStart = 20520, startLabel = "20:00", endLabel = "20:40"),
    AdapterItem(id = 5, title = "영어 공부", type = ItemType.Time, timePhase = TimeRangePhase.Upcoming, secondsUntilStart = 24120, startLabel = "21:00", endLabel = "21:30"),
    AdapterItem(
        id = 1, title = "기획안 마무리", percent = 30f,
        type = ItemType.Time, timePhase = TimeRangePhase.Active, remainingSeconds = 2520,
        startLabel = "14:00", endLabel = "15:00", currentLabel = "14:18",
        countdownText = "42분", dueText = "오후 3:00까지", leftString = "42분 남음",
        startString = "시작: 오후 2:00", endString = "종료: 오후 3:00",
        remainingSortKey = 2520, iconKey = "school"
    ),
    AdapterItem(
        id = 2, title = "가족과 함께하는 제주 여행 준비", percent = 62f, type = ItemType.Date, remainingDays = 12,
        countdownText = "D-12", dueText = "2026-10-01까지", leftString = "12일 남음",
        startLabel = "2026-09-01", endLabel = "2026-10-01",
        category = "여행", colorKey = "blue", iconKey = "flight", reminderText = "하루 전",
        remainingSortKey = 1_036_800
    ),
    AdapterItem(
        id = 3, type = ItemType.Date, remainingDays = -3, title = "완료한 일정", percent = 100f, countdownText = "D+3",
        dueText = "2026-09-16까지", leftString = "3일 지남", isExpired = true,
        startLabel = "2026-09-01", endLabel = "2026-09-16",
    )
).map(::withDetailFixture)

/** Original lengths for debug fixtures only; production facts come from ItemGenerate. */
private fun withDetailFixture(item: AdapterItem): AdapterItem {
    val inDays = item.remainingDays != null
    val range = if (inDays) TimeDetailRange.Calendar(java.time.LocalDate.parse(item.startLabel),
        java.time.LocalDate.parse(item.endLabel), includesFirstDay = item.type == null)
    else TimeDetailRange.Clock(java.time.LocalTime.parse(item.startLabel),
        java.time.LocalTime.parse(item.endLabel), java.time.LocalDate.of(2026, 9, 23))
    // Today keeps the production percentage denominator and 23:59:59 end separately.
    val total = if (!inDays && item.type == null) 86_400L else range.maximum
    val phase = item.timePhase ?: when {
        item.isExpired || item.remainingSeconds == 0L -> TimeRangePhase.Finished
        inDays && (item.remainingDays?.toLong() ?: 0) > range.maximum -> TimeRangePhase.Upcoming
        else -> TimeRangePhase.Active
    }
    val elapsed = when (phase) {
        TimeRangePhase.Upcoming -> 0L
        TimeRangePhase.Finished -> range.maximum
        TimeRangePhase.Active -> range.maximum - (if (inDays) item.remainingDays?.toLong() ?: 0 else item.remainingSeconds ?: 0)
    }.coerceIn(0, total)
    val percent = if (phase == TimeRangePhase.Upcoming) 0f else if (total > 0) elapsed.toFloat() / total * 100f else 100f
    return item.copy(percent = percent, detailFacts = TimeDetailFacts(percent, elapsed, total, phase,
        inDays = inDays, secondsLeft = item.remainingSeconds, secondsUntilStart = item.secondsUntilStart,
        includesToday = inDays && item.type == null, range = range,
        validRange = when (range) {
            is TimeDetailRange.Calendar -> !range.end.isBefore(range.start)
            is TimeDetailRange.Clock -> range.end.isAfter(range.start)
        }))
}

private fun homeDateFixtures(): List<AdapterItem> {
    val base = sampleItems.first { it.id == 2 }
    return listOf(
        base.copy(id = 20, title = "오늘까지 제출하는 원고", remainingDays = 0, startLabel = "2026-09-01", endLabel = "2026-09-23"),
        base.copy(id = 21, title = "다음 달에 시작하는 전시 준비", remainingDays = 17, startLabel = "2026-10-01", endLabel = "2026-10-10"),
        base.copy(id = 22, title = "당일 일정", remainingDays = 0, startLabel = "2026-09-23", endLabel = "2026-09-23"),
        base.copy(id = 23, title = "시작 전인 당일 일정", remainingDays = 10, startLabel = "2026-10-03", endLabel = "2026-10-03"),
        base.copy(id = 24, title = "확인이 필요한 날짜", remainingDays = 8, startLabel = "2026-10-04", endLabel = "2026-10-01"),
    ).map { withDetailFixture(it.copy(dueText = "", countdownText = "", leftString = "", category = "",
        remainingSortKey = it.remainingDays!!.toLong() * 86_400)) }
}

@Composable
private fun GalleryScreen(
    screen: String,
    paletteKey: String,
    themeMode: String = UserPreferences.THEME_AUTO,
    exportWidget: Boolean = false,
) {
    val longTitle = if (LocalContext.current.resources.configuration.locales[0].language == "en") "Review quarterly service metrics and prepare the work plan for next week" else "분기별 서비스 운영 지표를 검토하고 다음 주 업무 계획 정리하기"
    val periodItems = listOf(
        AdapterItem(title = "오늘", remainingSeconds = if (screen == "period-long") 86340 else if (screen == "period-boundary") 0 else 34920,
            leftString = "9시간 42분 남음", percent = when (screen) { "period-boundary" -> 100f; "period-long" -> 0.07f; else -> 59.58f },
            startLabel = "00:00", endLabel = "23:59:59"),
        AdapterItem(title = "이번 달", remainingDays = if (screen == "period-boundary") 0 else 12,
            leftString = "12일 남음", percent = if (screen == "period-boundary") 100f else 60f,
            startLabel = "2026-09-01", endLabel = "2026-09-30"),
        AdapterItem(title = "올해", remainingDays = if (screen == "period-long") 364 else if (screen == "period-boundary") 0 else 104,
            leftString = "104일 남음", percent = when (screen) { "period-boundary" -> 100f; "period-long" -> 0.27f; else -> 71.51f },
            startLabel = "2026-01-01", endLabel = "2026-12-31")
    ).map(::withDetailFixture)
    when {
        screen.startsWith("detail-") -> {
            val item = when {
                screen.contains("settle-month") -> withDetailFixture(periodItems[1].copy(remainingDays = 7))
                screen.contains("settle-year") -> withDetailFixture(periodItems[2].copy(remainingDays = 99))
                screen.contains("settle-today") -> withDetailFixture(periodItems[0].copy(remainingSeconds = 9 * 3_600L + 42 * 60 + 35))
                screen.contains("settle-small-gap") -> withDetailFixture(periodItems[1].copy(remainingDays = 28))
                screen.contains("borrow-hour") -> withDetailFixture(sampleItems.first { it.id == 1 }.copy(remainingSeconds = 42 * 60L + 35))
                screen.contains("borrow-minute") -> withDetailFixture(sampleItems.first { it.id == 1 }.copy(
                    endLabel = "14:02", remainingSeconds = 89))
                screen.contains("borrow-equal-minute") -> withDetailFixture(sampleItems.first { it.id == 1 }.copy(
                    endLabel = "15:10:10", remainingSeconds = 10 * 60L + 35))
                screen.contains("borrow-terminal") -> withDetailFixture(sampleItems.first { it.id == 1 }.copy(
                    endLabel = "14:01", remainingSeconds = 59))
                screen.contains("february") -> withDetailFixture(periodItems[1].copy(
                    startLabel = "2027-02-01", endLabel = "2027-02-28", remainingDays = 5))
                screen.contains("leap-month") -> withDetailFixture(periodItems[1].copy(
                    startLabel = "2028-02-01", endLabel = "2028-02-29", remainingDays = 6))
                screen.contains("leap-year") -> withDetailFixture(periodItems[2].copy(
                    startLabel = "2028-01-01", endLabel = "2028-12-31", remainingDays = 99))
                screen.contains("31-days") -> withDetailFixture(periodItems[1].copy(
                    startLabel = "2026-10-01", endLabel = "2026-10-31", remainingDays = 12))
                screen.contains("short-range") -> withDetailFixture(sampleItems.first { it.id == 1 }.copy(
                    startLabel = "14:00", endLabel = "14:00:45", remainingSeconds = 25))
                screen.contains("partial-minute") -> withDetailFixture(sampleItems.first { it.id == 1 }.copy(
                    startLabel = "14:00", endLabel = "14:05:30", remainingSeconds = 150))
                screen.contains("same-day") -> withDetailFixture(sampleItems.first { it.id == 2 }.copy(
                    startLabel = "2026-09-23", endLabel = "2026-09-23", remainingDays = 0))
                screen.contains("story-music") -> withDetailFixture(sampleItems.first { it.id == 2 }.copy(remainingDays = 25))
                screen.contains("story-literature") -> withDetailFixture(sampleItems.first { it.id == 2 }.copy(remainingDays = 42, endLabel = "2026-11-01"))
                screen.contains("story-seconds") -> withDetailFixture(sampleItems.first { it.id == 1 }.copy(remainingSeconds = 12))
                screen.contains("today") -> periodItems[0]
                screen.contains("month") -> periodItems[1]
                screen.contains("year") -> periodItems[2]
                screen.contains("waiting") -> sampleItems.first { it.id == 4 }
                screen.contains("expired") -> sampleItems.first { it.id == 3 }
                screen.contains("last-second") -> withDetailFixture(sampleItems.first { it.id == 1 }.copy(remainingSeconds = 1))
                else -> sampleItems.first { it.id == 1 }
            }.let { if (screen.contains("long")) it.copy(title = longTitle) else it }
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(item.title, style = MaterialTheme.typography.headlineSmall)
                    TimeDetailContent(item, when {
                        screen.contains("hidden") -> UserPreferences.PROGRESS_DISPLAY_HIDDEN
                        screen.contains("bar-only") -> UserPreferences.PROGRESS_DISPLAY_BAR_ONLY
                        else -> UserPreferences.PROGRESS_DISPLAY_FULL
                    })
                }
            }
        }
        screen == "picker-time" -> TimeLeftTimePicker(java.time.LocalTime.of(14, 0), "시작 시간", {}, {})
        screen == "picker-date" -> TimeLeftDatePicker(java.time.LocalDate.of(2026, 9, 18), {}, {})
        screen.startsWith("widget-configure") -> {
            val activity = LocalContext.current as? android.app.Activity
            val failNextLoad = remember(screen) { booleanArrayOf(screen.contains("error")) }
            val fixtures = remember(screen) {
                if (screen.contains("empty")) emptyList() else (1..(if (screen.contains("many")) 60 else 3)).map { id ->
                    ItemEntity(
                        id = id, type = if (id % 2 == 0) ItemType.Date else ItemType.Time,
                        title = if (id == 1) longTitle else if (id % 2 == 0) "여행 준비" else "집중 시간",
                        startValue = if (id % 2 == 0) "2026-09-01" else "14:0",
                        endValue = if (id % 2 == 0) "2026-10-01" else "15:0",
                        updateFlag = if (id % 2 == 0) RecurrenceMode.None else RecurrenceMode.TimeRange,
                        updateRate = 0,
                    )
                }
            }
            WidgetConfigureRoute(
                initial = WidgetConfiguration(
                    source = if (screen.contains("overview")) WidgetSource.Overview else WidgetSource.Custom,
                    itemId = if (screen.contains("deleted")) 900 else 1,
                    legacySummary = screen.contains("legacy"),
                ),
                dimensions = if (screen.contains("small")) WidgetDimensions(110, 74)
                    else WidgetDimensions.previewFor(if (screen.contains("overview")) WidgetSource.Overview else WidgetSource.Custom),
                paletteKey = paletteKey, themeMode = themeMode,
                loadItems = {
                    if (failNextLoad[0]) {
                        failNextLoad[0] = false
                        error("Debug widget loading failure")
                    }
                    fixtures
                },
                isEditing = screen.contains("legacy"), onBack = { activity?.finish() },
                onSave = { activity?.finish(); WidgetSaveResult.Saved },
            )
        }
        screen.startsWith("widget-") -> {
            val sizeName = when {
                screen.contains("4x1") -> "4x1"
                screen.contains("4x2") -> "4x2"
                screen.contains("small") -> "Compact"
                screen.contains("wide") -> "Wide"
                screen.contains("medium") -> "Medium"
                else -> "Large"
            }
            val width = when (sizeName) {
                "4x1", "4x2" -> if (screen.endsWith("min") || screen.contains("narrow")) 250 else 320
                "Compact" -> if (screen.endsWith("min")) 110 else 160
                "Medium" -> 250
                "Wide" -> 400
                else -> 360
            }
            val height = when (sizeName) {
                "4x1" -> if (screen.endsWith("min")) WidgetDimensions.OverviewMinimum.height else 96
                "4x2" -> if (screen.endsWith("min")) WidgetDimensions.Minimum.height else 200
                "Compact" -> if (screen.endsWith("min")) 74 else 100
                "Wide" -> 200
                "Medium" -> if (screen.contains("overview")) 240 else 200
                else -> 320
            }
            val base = LocalContext.current
            val config = android.content.res.Configuration(base.resources.configuration).apply { fontScale = LocalDensity.current.fontScale }
            val context = base.createConfigurationContext(config)
            val source = when {
                screen.contains("overview") -> WidgetSource.Overview
                screen.contains("today") -> WidgetSource.Today
                screen.contains("month") -> WidgetSource.Month
                screen.contains("year") -> WidgetSource.Year
                else -> WidgetSource.Custom
            }
            val item = when {
                screen.contains("missing") -> null
                screen.contains("waiting") -> sampleItems.first { it.id == 4 }
                screen.contains("expired") -> sampleItems.first { it.id == 3 }
                screen.contains("date") -> sampleItems.first { it.id == 2 }
                screen.contains("under-minute") -> withDetailFixture(sampleItems.first { it.id == 1 }.copy(remainingSeconds = 30))
                source == WidgetSource.Today -> periodItems[0]
                source == WidgetSource.Month -> periodItems[1]
                source == WidgetSource.Year -> periodItems[2]
                else -> sampleItems.first { it.id == 1 }
            }
            val views = AppWidget().previewViews(
                context, WidgetDimensions(width, height),
                WidgetConfiguration(source = source, itemId = 1, showRemaining = !screen.contains("countdown"), legacySummary = screen.contains("legacy")),
                item?.let { if (screen.contains("long")) it.copy(title = longTitle) else it }, periodItems, paletteKey, themeMode == UserPreferences.THEME_DARK,
                emptyMessage = R.string.widget_item_unavailable,
                showProgress = !screen.contains("hidden"),
                designKey = com.devidea.timeleft.ui.theme.LocalTimeLayout.current.key,
            )
            Surface(color = MaterialTheme.colorScheme.outlineVariant) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("RemoteViews · $sizeName · $width × $height", style = MaterialTheme.typography.bodySmall)
                    AndroidView(factory = {
                        views.apply(context, null).also { view ->
                            if (exportWidget) view.doOnLayout {
                                val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
                                view.draw(android.graphics.Canvas(bitmap))
                                java.io.File(context.cacheDir, "widget-preview-${sizeName.lowercase()}.png").outputStream().use {
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                                }
                            }
                        }
                    }, modifier = Modifier.width(width.dp).height(height.dp))
                }
            }
        }
        screen == "editor" || screen == "editor-time" -> {
            val type = if (screen == "editor-time") ItemType.Time else ItemType.Date
            ItemEditorScreen(
                initialType = type,
                initialItem = ItemEntity(
                    id = 1, type = type,
                    title = if (type == ItemType.Time) "집중해서 책 읽기" else "가족과 함께하는 제주 여행 준비",
                    startValue = if (type == ItemType.Time) "14:0" else "2026-09-01",
                    endValue = if (type == ItemType.Time) "15:0" else "2026-10-01",
                    updateFlag = if (type == ItemType.Time) RecurrenceMode.TimeRange else RecurrenceMode.None,
                    updateRate = 0, category = "개인", colorKey = "blue", iconKey = "school"
                ),
                defaultDateReminderOffset = ItemVisuals.REMINDER_DISABLED,
                defaultTimeReminderOffset = ItemVisuals.REMINDER_DISABLED,
                isLoading = false, isSaving = false, onBack = {}, onSave = {}
            )
        }
        screen == "settings" || screen == "settings-selected" || screen == "settings-legacy" || screen.startsWith("theme-preview") ->
            SettingsGallery(screen, themeMode, paletteKey, periodItems)
        else -> HomeScreen(
            initialSortValue = UserPreferences.SORT_NEAREST,
            initialLayoutValue = if (screen == "grid" || screen == "date-grid") UserPreferences.HOME_LAYOUT_GRID else UserPreferences.HOME_LAYOUT_LIST,
            expiredItemsMode = when (screen) {
                "dates-hide-expired" -> UserPreferences.EXPIRED_ITEMS_HIDE
                "date-items", "date-grid" -> UserPreferences.EXPIRED_ITEMS_BOTTOM
                else -> UserPreferences.EXPIRED_ITEMS_SHOW
            },
            progressDisplayMode = if (screen == "progress-hidden") UserPreferences.PROGRESS_DISPLAY_HIDDEN else UserPreferences.PROGRESS_DISPLAY_FULL,
            topItems = periodItems,
            customItems = when (screen) {
                "empty" -> emptyList()
                "idle" -> sampleItems.filter { it.timePhase != TimeRangePhase.Active }
                "dates" -> sampleItems.filter { it.type == ItemType.Date }
                "dates-many" -> sampleItems + (10..15).map { id ->
                    withDetailFixture(sampleItems.first { it.id == 4 }.copy(id = id, secondsUntilStart = id * 60L))
                } + homeDateFixtures()
                "date-items", "date-grid", "dates-hide-expired" -> sampleItems.filter { it.type == ItemType.Date } + homeDateFixtures()
                "dates-today" -> homeDateFixtures().filter { it.id == 20 }
                "dates-same-day" -> homeDateFixtures().filter { it.id == 22 }
                "dates-future" -> homeDateFixtures().filter { it.id == 21 || it.id == 23 }
                "dates-expired" -> sampleItems.filter { it.isExpired }
                "dates-invalid" -> homeDateFixtures().filter { it.id == 24 }
                "overlap" -> sampleItems + withDetailFixture(sampleItems.first { it.id == 1 }.copy(id = 6, title = "함께 진행하는 업무", remainingSeconds = 1200, percent = 66.67f, startLabel = "13:38", endLabel = "14:38"))
                "long" -> sampleItems.map { if (it.id == 1) it.copy(title = longTitle) else it }
                else -> sampleItems
            },
            initialShowAll = screen == "grid" || screen == "items" || screen == "date-items" || screen == "date-grid",
            onOpenSettings = {}, onSortChange = {}, onLayoutChange = {},
            onStartFocus = {}, onOpenFocus = {}, onMorePeriods = {},
            onAddTime = {}, onAddDate = {}, onEditItem = {}, onDeleteItem = {}
        )
    }
}

/** Real settings controls with local state only; no saved preferences or reminders are changed. */
@Composable
private fun SettingsGallery(screen: String, initialTheme: String, initialPalette: String, periods: List<AdapterItem>) {
    val initialDesign = com.devidea.timeleft.ui.theme.LocalTimeLayout.current.key
    var design by rememberSaveable(screen) { mutableStateOf(initialDesign) }
    var usage by rememberSaveable(screen) { mutableStateOf(false) }
    var diagnostics by rememberSaveable(screen) { mutableStateOf(false) }
    val selectedFixture = screen == "settings-selected"
    var theme by rememberSaveable(screen, initialTheme) { mutableStateOf(initialTheme) }
    var palette by rememberSaveable(screen, initialPalette) { mutableStateOf(initialPalette) }
    var sort by rememberSaveable(screen) {
        mutableStateOf(if (selectedFixture) UserPreferences.SORT_PROGRESS else UserPreferences.SORT_NEAREST)
    }
    var expired by rememberSaveable(screen) {
        mutableStateOf(if (selectedFixture) UserPreferences.EXPIRED_ITEMS_BOTTOM else UserPreferences.EXPIRED_ITEMS_SHOW)
    }
    var progress by rememberSaveable(screen) {
        mutableStateOf(when (screen) {
            "settings-legacy" -> UserPreferences.PROGRESS_DISPLAY_BAR_ONLY
            "settings-selected" -> UserPreferences.PROGRESS_DISPLAY_HIDDEN
            else -> UserPreferences.PROGRESS_DISPLAY_FULL
        })
    }
    var dateReminder by rememberSaveable(screen) { mutableStateOf(if (selectedFixture) 7 else ItemVisuals.REMINDER_DISABLED) }
    var timeReminder by rememberSaveable(screen) { mutableStateOf(if (selectedFixture) 60 else ItemVisuals.REMINDER_DISABLED) }
    var previewOpen by rememberSaveable(screen) { mutableStateOf(screen.startsWith("theme-preview")) }
    var previewStatus by rememberSaveable(screen) { mutableStateOf(when (screen) {
        "theme-preview-loading" -> "loading"
        "theme-preview-error" -> "error"
        else -> "ready"
    }) }
    val previewCapturedAt = remember(screen) { System.currentTimeMillis() }
    val previewState = ThemePreviewState(
        snapshot = if (previewStatus == "ready") ThemePreviewSnapshot(periods,
            if (screen == "theme-preview-empty") emptyList() else sampleItems, previewCapturedAt) else null,
        loading = previewStatus == "loading", failed = previewStatus == "error",
    )
    TimeLeftTheme(themeMode = theme, paletteKey = palette, designKey = design) {
        if (previewOpen) ThemePreviewScreen(
            initialThemeMode = theme, initialPaletteKey = palette,
            initialDesignKey = design,
            systemDark = systemUsesDarkTheme(LocalContext.current),
            state = previewState, expiredItemsMode = expired, progressDisplayMode = progress,
            onBack = { previewOpen = false }, onRetry = { previewStatus = "ready" },
            onApply = { choice -> previewOpen = false; theme = choice.mode; palette = choice.paletteKey; design = choice.designKey },
        ) else SettingsScreen(
            themeMode = theme, paletteKey = palette, homeSort = sort, expiredItemsMode = expired,
            progressDisplayMode = progress, defaultDateReminderOffset = dateReminder, defaultTimeReminderOffset = timeReminder,
            dateReminderTime = "09:00", remindersEnabled = !selectedFixture, versionName = "디자인 검수",
            onBack = {}, onOpenThemePreview = { previewOpen = true }, previewPeriods = periods, onSortSelected = { sort = it },
            onExpiredItemsModeSelected = { expired = it }, onProgressDisplayModeSelected = { progress = it },
            onDefaultDateReminderSelected = { dateReminder = it }, onDefaultTimeReminderSelected = { timeReminder = it },
            onSelectDateReminderTime = {}, onOpenNotificationSettings = {}, onOpenPrivacyPolicy = {},
            usageEnabled = usage, diagnosticsEnabled = diagnostics,
            onUsageChanged = { usage = it }, onDiagnosticsChanged = { diagnostics = it },
        )
    }
}

class PalettePreviewProvider : PreviewParameterProvider<ThemePalette> {
    override val values = ThemePalette.entries.asSequence()
}

@Preview(name = "Light", widthDp = 360, heightDp = 800, locale = "ko")
@Preview(name = "Dark", widthDp = 360, heightDp = 800, locale = "ko", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 320, heightDp = 800, locale = "ko", fontScale = 1.5f)
private annotation class DesignPreviews

@Composable
private fun GalleryPreview(screen: String, palette: ThemePalette) {
    TimeLeftTheme(paletteKey = palette.key) { GalleryScreen(screen, palette.key) }
}

@DesignPreviews
@Composable
private fun HomePreview(@PreviewParameter(PalettePreviewProvider::class) palette: ThemePalette) =
    GalleryPreview("home", palette)

@DesignPreviews
@Composable
private fun DatesHomePreview(@PreviewParameter(PalettePreviewProvider::class) palette: ThemePalette) =
    GalleryPreview("dates", palette)

@DesignPreviews
@Composable
private fun TimeDetailPreview(@PreviewParameter(PalettePreviewProvider::class) palette: ThemePalette) =
    GalleryPreview("detail-time", palette)

@DesignPreviews
@Composable
private fun GridPreview(@PreviewParameter(PalettePreviewProvider::class) palette: ThemePalette) =
    GalleryPreview("grid", palette)

@DesignPreviews
@Composable
private fun EmptyPreview(@PreviewParameter(PalettePreviewProvider::class) palette: ThemePalette) =
    GalleryPreview("empty", palette)

@DesignPreviews
@Composable
private fun EditorPreview(@PreviewParameter(PalettePreviewProvider::class) palette: ThemePalette) =
    GalleryPreview("editor", palette)

@DesignPreviews
@Composable
private fun TimeEditorPreview(@PreviewParameter(PalettePreviewProvider::class) palette: ThemePalette) =
    GalleryPreview("editor-time", palette)

@DesignPreviews
@Composable
private fun SettingsPreview(@PreviewParameter(PalettePreviewProvider::class) palette: ThemePalette) =
    GalleryPreview("settings", palette)

@DesignPreviews
@Composable
private fun ThemePickerPreview(@PreviewParameter(PalettePreviewProvider::class) palette: ThemePalette) =
    GalleryPreview("theme-preview", palette)

class EnglishDesignGalleryActivity : DesignGalleryActivity() {
    override val galleryLocale: Locale get() = Locale.ENGLISH
}
