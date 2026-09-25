package com.devidea.timeleft.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences

/** Brightness and accent are user choices; a design owns its visual definitions. */
internal data class ThemeSelection(val mode: String, val paletteKey: String) {
    fun normalized() = copy(
        mode = mode.takeIf { it in listOf(UserPreferences.THEME_AUTO, UserPreferences.THEME_LIGHT, UserPreferences.THEME_DARK) }
            ?: UserPreferences.THEME_AUTO,
        paletteKey = ThemePalette.fromKey(paletteKey).key,
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
)

/** Add another approved design here when its actual app/widget presentation is ready. */
internal object TimeLeftThemes {
    val TimeFocus = ThemeDefinition(
        id = "time-focus", version = 1, nameRes = R.string.theme_time_focus,
        palettes = ThemePalette.entries.toList(), typography = TimeLeftTypography, shapes = TimeLeftShapes,
        colors = { palette, dark -> if (dark) palette.darkColors else palette.lightColors },
    )
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
    val definition = TimeLeftThemes.TimeFocus
    val palette = definition.palettes.firstOrNull { it.key == choice.paletteKey } ?: definition.palettes.first()
    val dark = when (choice.mode) {
        UserPreferences.THEME_LIGHT -> false
        UserPreferences.THEME_DARK -> true
        else -> systemDark
    }
    return ResolvedTheme(definition, palette, dark, definition.colors(palette, dark))
}
