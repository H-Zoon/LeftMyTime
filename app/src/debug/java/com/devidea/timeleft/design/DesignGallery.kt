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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.devidea.timeleft.ui.components.TimeLeftDatePicker
import com.devidea.timeleft.ui.components.TimeLeftTimePicker
import com.devidea.timeleft.widget.AppWidget
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.ItemVisuals
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.editor.ItemEditorScreen
import com.devidea.timeleft.ui.home.HomeScreen
import com.devidea.timeleft.ui.settings.SettingsScreen
import com.devidea.timeleft.ui.theme.ThemePalette
import com.devidea.timeleft.ui.theme.TimeLeftTheme
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
        val fontScale = intent.getFloatExtra("fontScale", 1f).coerceIn(1f, 2f)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = mode != UserPreferences.THEME_DARK
            isAppearanceLightNavigationBars = mode != UserPreferences.THEME_DARK
        }
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TimeLeftTheme(themeMode = mode, paletteKey = palette) {
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
        category = "여행", colorKey = "blue", iconKey = "flight", reminderText = "하루 전",
        remainingSortKey = 1_036_800
    ),
    AdapterItem(
        id = 3, type = ItemType.Date, remainingDays = -3, title = "완료한 일정", percent = 100f, countdownText = "D+3",
        dueText = "2026-09-16까지", leftString = "3일 지남", isExpired = true
    )
)

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
    )
    when {
        screen == "picker-time" -> TimeLeftTimePicker(java.time.LocalTime.of(14, 0), "시작 시간", {}, {})
        screen == "picker-date" -> TimeLeftDatePicker(java.time.LocalDate.of(2026, 9, 18), {}, {})
        screen.startsWith("widget-") -> {
            val sizeName = when {
                screen.contains("small") -> "Compact"
                screen.contains("wide") -> "Wide"
                screen.contains("medium") -> "Medium"
                else -> "Large"
            }
            val width = when (sizeName) { "Compact" -> if (screen.endsWith("min")) 110 else 160; "Medium" -> 220; else -> 300 }
            val height = when (sizeName) { "Compact" -> if (screen.endsWith("min")) 74 else 100; "Wide" -> 88; "Medium" -> 200; else -> 320 }
            val base = LocalContext.current
            val config = android.content.res.Configuration(base.resources.configuration).apply { fontScale = LocalDensity.current.fontScale }
            val context = base.createConfigurationContext(config)
            val views = AppWidget().previewViews(context, sizeName, sampleItems.first { it.id == 1 }, periodItems, paletteKey, themeMode == UserPreferences.THEME_DARK, height)
            Surface(color = MaterialTheme.colorScheme.outlineVariant) {
                Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
        screen == "settings" -> SettingsScreen(
            themeMode = themeMode,
            paletteKey = paletteKey,
            homeSort = UserPreferences.SORT_NEAREST,
            expiredItemsMode = UserPreferences.EXPIRED_ITEMS_SHOW,
            progressDisplayMode = UserPreferences.PROGRESS_DISPLAY_FULL,
            defaultDateReminderOffset = ItemVisuals.REMINDER_DISABLED,
            defaultTimeReminderOffset = ItemVisuals.REMINDER_DISABLED,
            dateReminderTime = "09:00", remindersEnabled = true, versionName = "디자인 검수",
            onBack = {}, onThemeSelected = {}, onPaletteSelected = {}, onSortSelected = {},
            onExpiredItemsModeSelected = {}, onProgressDisplayModeSelected = {},
            onDefaultDateReminderSelected = {}, onDefaultTimeReminderSelected = {},
            onSelectDateReminderTime = {}, onOpenNotificationSettings = {}, onOpenPrivacyPolicy = {}
        )
        else -> HomeScreen(
            initialSortValue = UserPreferences.SORT_NEAREST,
            initialLayoutValue = if (screen == "grid") UserPreferences.HOME_LAYOUT_GRID else UserPreferences.HOME_LAYOUT_LIST,
            expiredItemsMode = UserPreferences.EXPIRED_ITEMS_SHOW,
            progressDisplayMode = if (screen == "progress-hidden") UserPreferences.PROGRESS_DISPLAY_HIDDEN else UserPreferences.PROGRESS_DISPLAY_FULL,
            topItems = periodItems,
            customItems = when (screen) {
                "empty" -> emptyList()
                "idle" -> sampleItems.filter { it.timePhase != TimeRangePhase.Active }
                "dates" -> sampleItems.filter { it.type == ItemType.Date }
                "overlap" -> sampleItems + sampleItems.first { it.id == 1 }.copy(id = 6, title = "함께 진행하는 업무", remainingSeconds = 1200, percent = 66.67f, startLabel = "13:38", endLabel = "14:38")
                "long" -> sampleItems.map { if (it.id == 1) it.copy(title = longTitle) else it }
                else -> sampleItems
            },
            initialShowAll = screen == "grid" || screen == "items",
            onOpenSettings = {}, onSortChange = {}, onLayoutChange = {},
            onAddTime = {}, onAddDate = {}, onEditItem = {}, onDeleteItem = {}
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

class EnglishDesignGalleryActivity : DesignGalleryActivity() {
    override val galleryLocale: Locale get() = Locale.ENGLISH
}
