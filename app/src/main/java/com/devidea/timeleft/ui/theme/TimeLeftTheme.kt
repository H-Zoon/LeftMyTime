package com.devidea.timeleft.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences

/** All palettes share A's quiet surfaces and hierarchy; saved accents remain available. */
enum class ThemePalette(
    val key: String,
    @param:StringRes val labelRes: Int,
    val lightAccent: Color,
    val darkAccent: Color,
) {
    Clay(UserPreferences.COLOR_THEME_CLAY, R.string.settings_palette_clay, Color(0xFFB5482D), Color(0xFFEE9278)),
    Indigo(UserPreferences.COLOR_THEME_INDIGO, R.string.settings_palette_indigo, Color(0xFF4F46E5), Color(0xFFA5B4FC)),
    Emerald(UserPreferences.COLOR_THEME_EMERALD, R.string.settings_palette_emerald, Color(0xFF047857), Color(0xFF6EE7B7)),
    Rose(UserPreferences.COLOR_THEME_ROSE, R.string.settings_palette_rose, Color(0xFFC7254B), Color(0xFFFDA4AF)),
    Amber(UserPreferences.COLOR_THEME_AMBER, R.string.settings_palette_amber, Color(0xFF99520B), Color(0xFFFCD34D)),
    Slate(UserPreferences.COLOR_THEME_SLATE, R.string.settings_palette_slate, Color(0xFF334155), Color(0xFFCBD5E1));

    val lightColors: ColorScheme get() = timeLeftColors(false, lightAccent)
    val darkColors: ColorScheme get() = timeLeftColors(true, darkAccent)

    companion object {
        fun fromKey(key: String): ThemePalette = entries.firstOrNull { it.key == key } ?: Clay
    }
}

private fun timeLeftColors(dark: Boolean, accent: Color): ColorScheme {
    val background = if (dark) Color(0xFF181B19) else Color(0xFFF7F7F2)
    val ink = if (dark) Color(0xFFEDF0E9) else Color(0xFF242924)
    val muted = if (dark) Color(0xFFA7AFA5) else Color(0xFF646B64)
    val rule = if (dark) Color(0xFF3C443C) else Color(0xFFD8DDD4)
    val surface = if (dark) Color(0xFF242A25) else Color.White
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent, onPrimary = if (dark) background else Color.White,
        primaryContainer = rule, onPrimaryContainer = ink,
        secondary = accent, onSecondary = if (dark) background else Color.White,
        secondaryContainer = rule, onSecondaryContainer = ink,
        tertiary = accent, onTertiary = if (dark) background else Color.White,
        tertiaryContainer = rule, onTertiaryContainer = ink,
        background = background, onBackground = ink,
        surface = surface, onSurface = ink,
        surfaceVariant = rule, onSurfaceVariant = muted,
        outline = muted, outlineVariant = rule, surfaceTint = Color.Transparent,
        inverseSurface = ink, inverseOnSurface = background,
        inversePrimary = if (dark) Color(0xFFB5482D) else Color(0xFFEE9278),
        error = if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
        onError = if (dark) Color(0xFF690005) else Color.White,
        errorContainer = if (dark) Color(0xFF93000A) else Color(0xFFFFDAD6),
        onErrorContainer = if (dark) Color(0xFFFFDAD6) else Color(0xFF410002)
    )
}

private fun textStyle(size: Int, line: Int, medium: Boolean = false) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = if (medium) FontWeight.Medium else FontWeight.Normal
)

val TimeLeftTypography = Typography(
    displayLarge = textStyle(96, 104).copy(fontFeatureSettings = "tnum", letterSpacing = (-4).sp),
    displayMedium = textStyle(56, 64).copy(fontFeatureSettings = "tnum", letterSpacing = (-2).sp),
    displaySmall = textStyle(30, 38).copy(fontFeatureSettings = "tnum"),
    headlineLarge = textStyle(28, 36, true),
    headlineMedium = textStyle(24, 32, true),
    headlineSmall = textStyle(22, 30, true),
    titleLarge = textStyle(22, 30, true),
    titleMedium = textStyle(16, 24, true),
    titleSmall = textStyle(14, 20, true),
    bodyLarge = textStyle(16, 24),
    bodyMedium = textStyle(14, 22),
    bodySmall = textStyle(12, 18),
    labelLarge = textStyle(14, 20, true),
    labelMedium = textStyle(12, 18, true),
    labelSmall = textStyle(12, 18)
)

internal val TimeLeftShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun TimeLeftTheme(
    themeMode: String = UserPreferences.THEME_AUTO,
    paletteKey: String = UserPreferences.COLOR_THEME_CLAY,
    content: @Composable () -> Unit
) {
    val theme = resolveTheme(ThemeSelection(themeMode, paletteKey), isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = theme.colors,
        typography = theme.definition.typography,
        shapes = theme.definition.shapes,
        content = content
    )
}
