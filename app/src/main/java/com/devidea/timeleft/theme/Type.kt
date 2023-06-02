package com.devidea.timeleft.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devidea.timeleft.R


val nanum_nomal = FontFamily(
    Font(R.font.nanum_square_round_l),
    Font(R.font.nanum_square_round_r)
)

val nanum_bold = FontFamily(
    Font(R.font.nanum_square_round_eb),
    Font(R.font.nanum_square_round_b)
)

// Set of Material typography styles to start with
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = nanum_nomal,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),

    displayMedium = TextStyle(
        fontFamily = nanum_nomal,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),

    displaySmall = TextStyle(
        fontFamily = nanum_nomal,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    )
)
