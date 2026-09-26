package com.devidea.timeleft.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences

/** Brightness and accent are user choices; a design owns its visual definitions. */
internal data class ThemeSelection(val mode: String, val paletteKey: String,
    val designKey: String = UserPreferences.DESIGN_TIME_FOCUS) {
    fun normalized() = copy(
        mode = mode.takeIf { it in listOf(UserPreferences.THEME_AUTO, UserPreferences.THEME_LIGHT, UserPreferences.THEME_DARK) }
            ?: UserPreferences.THEME_AUTO,
        paletteKey = ThemePalette.fromKey(paletteKey).key,
        designKey = TimeLayout.fromKey(designKey).key,
    )
}

internal data class ThemeDefinition(
    val id: String,
    val version: Int,
    @param:StringRes val nameRes: Int,
    val palettes: List<ThemePalette>,
    val typography: Typography,
    val shapes: Shapes,
    val colors: (ThemePalette, Boolean) -> ColorScheme,
    val layout: TimeLayout,
)

internal enum class TimeLayout(val key: String, @param:StringRes val labelRes: Int) {
    TimeFocus(UserPreferences.DESIGN_TIME_FOCUS, R.string.theme_time_focus),
    TimeBoard(UserPreferences.DESIGN_TIME_BOARD, R.string.theme_time_board);

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: TimeFocus
    }
}

internal val LocalTimeLayout = androidx.compose.runtime.staticCompositionLocalOf { TimeLayout.TimeFocus }

/** Add another approved design here when its actual app/widget presentation is ready. */
internal object TimeLeftThemes {
    val TimeFocus = ThemeDefinition(
        id = "time-focus", version = 1, nameRes = R.string.theme_time_focus,
        palettes = ThemePalette.entries.toList(), typography = TimeLeftTypography, shapes = TimeLeftShapes,
        colors = { palette, dark -> if (dark) palette.darkColors else palette.lightColors },
        layout = TimeLayout.TimeFocus,
    )
    val TimeBoard = TimeFocus.copy(id = UserPreferences.DESIGN_TIME_BOARD,
        nameRes = R.string.theme_time_board, layout = TimeLayout.TimeBoard)
}

internal data class ResolvedTheme(
    val definition: ThemeDefinition,
    val palette: ThemePalette,
    val dark: Boolean,
    val colors: ColorScheme,
)

/** Shared by the app, draft previews and RemoteViews; does not read or write preferences. */
internal fun resolveTheme(selection: ThemeSelection, systemDark: Boolean): ResolvedTheme {
    val choice = selection.normalized()
    val definition = if (choice.designKey == UserPreferences.DESIGN_TIME_BOARD) TimeLeftThemes.TimeBoard else TimeLeftThemes.TimeFocus
    val palette = definition.palettes.firstOrNull { it.key == choice.paletteKey } ?: definition.palettes.first()
    val dark = when (choice.mode) {
        UserPreferences.THEME_LIGHT -> false
        UserPreferences.THEME_DARK -> true
        else -> systemDark
    }
    return ResolvedTheme(definition, palette, dark, definition.colors(palette, dark))
}
