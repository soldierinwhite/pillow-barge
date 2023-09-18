package io.soldierinwhite.pillowbarge.util

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Dp.fromWidth(): WindowWidthSizeClass {
    require(this >= 0.dp) { "Width must not be negative" }
    return when {
        this < 600.dp -> WindowWidthSizeClass.Compact
        this < 840.dp -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }
}
