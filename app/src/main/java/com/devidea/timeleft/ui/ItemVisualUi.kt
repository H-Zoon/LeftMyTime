package com.devidea.timeleft.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.devidea.timeleft.ItemVisuals

@Composable
fun itemAccentColor(
    key: String,
    fallback: Color,
): Color {
    if (key == ItemVisuals.AUTO_COLOR_KEY) return fallback
    val color = Color(ItemVisuals.colorInt(key))
    // User colors were chosen for light surfaces. Lift the same hue for dark
    // metadata chips; the stored color key and color-picker swatch stay intact.
    return if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        lerp(color, Color.White, 0.45f)
    } else {
        color
    }
}

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
