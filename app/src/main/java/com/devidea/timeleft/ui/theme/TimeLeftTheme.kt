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
    primary = Color(0xFF146C94),
    onPrimary = Color.White,
    secondary = Color(0xFF2E7D5B),
    tertiary = Color(0xFFB85C38),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF171A1F),
    surface = Color.White,
    onSurface = Color(0xFF171A1F),
    surfaceVariant = Color(0xFFE8EAEE),
    onSurfaceVariant = Color(0xFF515967),
    outline = Color(0xFFC8CDD4)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ED5F3),
    onPrimary = Color(0xFF003546),
    secondary = Color(0xFF8DDBB5),
    tertiary = Color(0xFFFFB59B),
    background = Color(0xFF111316),
    onBackground = Color(0xFFE9EDF2),
    surface = Color(0xFF1B1F24),
    onSurface = Color(0xFFE9EDF2),
    surfaceVariant = Color(0xFF2A3037),
    onSurfaceVariant = Color(0xFFC6CDD6),
    outline = Color(0xFF444C56)
)

private val TimeLeftTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 42.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NanumSquareRound,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
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
