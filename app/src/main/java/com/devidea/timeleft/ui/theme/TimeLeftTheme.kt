package com.devidea.timeleft.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences

private val NanumSquareRound = FontFamily(
    Font(R.font.nanum_square_round_r, FontWeight.Normal),
    Font(R.font.nanum_square_round_b, FontWeight.Bold),
    Font(R.font.nanum_square_round_eb, FontWeight.ExtraBold)
)

enum class ThemePalette(
    val key: String,
    @param:StringRes val labelRes: Int,
    val lightColors: ColorScheme,
    val darkColors: ColorScheme,
) {
    Indigo(
        key = UserPreferences.COLOR_THEME_INDIGO,
        labelRes = R.string.settings_palette_indigo,
        lightColors = lightColorScheme(
            primary = Color(0xFF4F46E5),
            onPrimary = Color.White,
            secondary = Color(0xFF16A34A),
            secondaryContainer = Color(0xFFE0E7FF),
            onSecondaryContainer = Color(0xFF312E81),
            tertiary = Color(0xFFD97706),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF64748B),
            outline = Color(0xFFCBD5E1)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFA5B4FC),
            onPrimary = Color(0xFF312E81),
            secondary = Color(0xFF86EFAC),
            secondaryContainer = Color(0xFF3730A3),
            onSecondaryContainer = Color(0xFFE0E7FF),
            tertiary = Color(0xFFFBBF24),
            background = Color(0xFF0F172A),
            onBackground = Color(0xFFE2E8F0),
            surface = Color(0xFF111827),
            onSurface = Color(0xFFE5E7EB),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF334155)
        )
    ),
    Emerald(
        key = UserPreferences.COLOR_THEME_EMERALD,
        labelRes = R.string.settings_palette_emerald,
        lightColors = lightColorScheme(
            primary = Color(0xFF047857),
            onPrimary = Color.White,
            secondary = Color(0xFF0F766E),
            secondaryContainer = Color(0xFFD1FAE5),
            onSecondaryContainer = Color(0xFF065F46),
            tertiary = Color(0xFFD97706),
            background = Color(0xFFF5FBF8),
            onBackground = Color(0xFF0F241E),
            surface = Color.White,
            onSurface = Color(0xFF0F241E),
            surfaceVariant = Color(0xFFDCEFE7),
            onSurfaceVariant = Color(0xFF526B63),
            outline = Color(0xFFB8D7CB)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFF6EE7B7),
            onPrimary = Color(0xFF064E3B),
            secondary = Color(0xFF5EEAD4),
            secondaryContainer = Color(0xFF065F46),
            onSecondaryContainer = Color(0xFFD1FAE5),
            tertiary = Color(0xFFFBBF24),
            background = Color(0xFF081C17),
            onBackground = Color(0xFFD7EEE6),
            surface = Color(0xFF102620),
            onSurface = Color(0xFFE0F2EC),
            surfaceVariant = Color(0xFF183A31),
            onSurfaceVariant = Color(0xFF9CBCAF),
            outline = Color(0xFF285548)
        )
    ),
    Rose(
        key = UserPreferences.COLOR_THEME_ROSE,
        labelRes = R.string.settings_palette_rose,
        lightColors = lightColorScheme(
            primary = Color(0xFFE11D48),
            onPrimary = Color.White,
            secondary = Color(0xFFDB2777),
            secondaryContainer = Color(0xFFFFE4E6),
            onSecondaryContainer = Color(0xFF9F1239),
            tertiary = Color(0xFFF97316),
            background = Color(0xFFFFF7F8),
            onBackground = Color(0xFF2B1118),
            surface = Color.White,
            onSurface = Color(0xFF2B1118),
            surfaceVariant = Color(0xFFFCE1E7),
            onSurfaceVariant = Color(0xFF76535B),
            outline = Color(0xFFF2C0CB)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFFDA4AF),
            onPrimary = Color(0xFF881337),
            secondary = Color(0xFFF9A8D4),
            secondaryContainer = Color(0xFF881337),
            onSecondaryContainer = Color(0xFFFFE4E6),
            tertiary = Color(0xFFFDBA74),
            background = Color(0xFF241015),
            onBackground = Color(0xFFF4DDE2),
            surface = Color(0xFF30171D),
            onSurface = Color(0xFFFBE7EB),
            surfaceVariant = Color(0xFF49242D),
            onSurfaceVariant = Color(0xFFD4A0AC),
            outline = Color(0xFF663541)
        )
    ),
    Amber(
        key = UserPreferences.COLOR_THEME_AMBER,
        labelRes = R.string.settings_palette_amber,
        lightColors = lightColorScheme(
            primary = Color(0xFFB45309),
            onPrimary = Color.White,
            secondary = Color(0xFFCA8A04),
            secondaryContainer = Color(0xFFFEF3C7),
            onSecondaryContainer = Color(0xFF92400E),
            tertiary = Color(0xFFDC2626),
            background = Color(0xFFFFFBF2),
            onBackground = Color(0xFF291A0A),
            surface = Color.White,
            onSurface = Color(0xFF291A0A),
            surfaceVariant = Color(0xFFFCEBC7),
            onSurfaceVariant = Color(0xFF725F41),
            outline = Color(0xFFEBCF91)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFFCD34D),
            onPrimary = Color(0xFF78350F),
            secondary = Color(0xFFFDE68A),
            secondaryContainer = Color(0xFF78350F),
            onSecondaryContainer = Color(0xFFFEF3C7),
            tertiary = Color(0xFFFCA5A5),
            background = Color(0xFF22190B),
            onBackground = Color(0xFFF4E8CD),
            surface = Color(0xFF302312),
            onSurface = Color(0xFFFFF3D6),
            surfaceVariant = Color(0xFF49351B),
            onSurfaceVariant = Color(0xFFD1B987),
            outline = Color(0xFF654A27)
        )
    ),
    Slate(
        key = UserPreferences.COLOR_THEME_SLATE,
        labelRes = R.string.settings_palette_slate,
        lightColors = lightColorScheme(
            primary = Color(0xFF334155),
            onPrimary = Color.White,
            secondary = Color(0xFF475569),
            secondaryContainer = Color(0xFFE2E8F0),
            onSecondaryContainer = Color(0xFF1E293B),
            tertiary = Color(0xFF0F766E),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF64748B),
            outline = Color(0xFFCBD5E1)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFCBD5E1),
            onPrimary = Color(0xFF1E293B),
            secondary = Color(0xFF94A3B8),
            secondaryContainer = Color(0xFF334155),
            onSecondaryContainer = Color(0xFFE2E8F0),
            tertiary = Color(0xFF5EEAD4),
            background = Color(0xFF0F172A),
            onBackground = Color(0xFFE2E8F0),
            surface = Color(0xFF111827),
            onSurface = Color(0xFFE5E7EB),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF334155)
        )
    );

    companion object {
        fun fromKey(key: String): ThemePalette =
            entries.firstOrNull { it.key == key } ?: Indigo
    }
}

private val TimeLeftTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

private val TimeLeftShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TimeLeftTheme(
    themeMode: String = UserPreferences.THEME_AUTO,
    paletteKey: String = UserPreferences.COLOR_THEME_INDIGO,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        UserPreferences.THEME_LIGHT -> false
        UserPreferences.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val palette = ThemePalette.fromKey(paletteKey)

    MaterialTheme(
        colorScheme = if (darkTheme) palette.darkColors else palette.lightColors,
        typography = TimeLeftTypography,
        shapes = TimeLeftShapes,
        content = content
    )
}
