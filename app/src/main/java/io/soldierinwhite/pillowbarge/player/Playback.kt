package io.soldierinwhite.pillowbarge.player

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    contentDescription: String?
) {
    IconButton(
        modifier = modifier
            .size(56.dp),
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(icon),
            modifier = Modifier.size(56.dp),
            contentDescription = contentDescription
        )
    }
}
