package com.devidea.timeleft.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devidea.timeleft.R

private val NanumSquareRound = FontFamily(
    Font(R.font.nanum_square_round_r, FontWeight.Normal),
    Font(R.font.nanum_square_round_b, FontWeight.Bold),
    Font(R.font.nanum_square_round_eb, FontWeight.ExtraBold)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    secondary = Color(0xFF16A34A),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF312E81),
    secondary = Color(0xFF86EFAC),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

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
    labelLarge = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun TimeLeftTheme(
    themeMode: String = "auto",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TimeLeftTypography,
        content = content
    )
}
