package com.devidea.timeleft.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.devidea.timeleft.ItemVisuals

@Composable
fun itemAccentColor(
    key: String,
    fallback: Color,
): Color =
    if (key == ItemVisuals.AUTO_COLOR_KEY) fallback else Color(ItemVisuals.colorInt(key))

fun itemIconVector(key: String): ImageVector =
    when (key) {
        "work" -> Icons.Filled.Work
        "school" -> Icons.Filled.School
        "flight" -> Icons.Filled.Flight
        "cake" -> Icons.Filled.Cake
        "fitness" -> Icons.Filled.FitnessCenter
        "favorite" -> Icons.Filled.Favorite
        else -> Icons.Filled.Event
    }
