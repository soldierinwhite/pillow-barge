package io.soldierinwhite.pillowbarge.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.Light
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.text.font.FontWeight.Companion.Normal
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import io.soldierinwhite.pillowbarge.R

val quicksand = FontFamily(
    Font(R.font.quicksand_bold, Bold),
    Font(R.font.quicksand_semibold, SemiBold),
    Font(R.font.quicksand_regular, Normal),
    Font(R.font.quicksand_medium, Medium),
    Font(R.font.quicksand_light, Light)
)

val patrickHand = FontFamily(
    Font(R.font.patrickhand_regular, Normal)
)
