package io.soldierinwhite.pillowbarge.player

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.soldierinwhite.pillowbarge.R
import io.soldierinwhite.pillowbarge.home.PlaybackState
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryType
import io.soldierinwhite.pillowbarge.ui.theme.PillowBargeTheme
import io.soldierinwhite.pillowbarge.util.fromWidth


@Composable
fun Player(
    modifier: Modifier = Modifier,
    currentlyPlayingStory: Story,
    playlist: List<Story>,
    playbackState: PlaybackState,
    onPrevious: () -> Unit,
    onSeekBackward: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeekForward: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = currentlyPlayingStory.imageUri?.let { Uri.parse(it) }?.path?.let {
                try {
                    BitmapPainter(BitmapFactory.decodeFile(it).asImageBitmap())
                } catch (e: Exception) {
                    null
                }
            } ?: painterResource(id = R.drawable.book),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (isSystemInDarkTheme()) 0.25f else 0.5f,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 48.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = currentlyPlayingStory.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = currentlyPlayingStory.voicedBy,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    imageVector = Icons.Default.Close,
                    contentDescription = "Stop",
                    tint = Color.White
                )
            }
        }
        Row(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedVisibility(playlist.firstOrNull()?.id != currentlyPlayingStory.id) {
                IconButton(modifier = Modifier.size(48.dp), onClick = onPrevious) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        painter = painterResource(id = R.drawable.previous),
                        contentDescription = stringResource(R.string.previous),
                        tint = Color.White
                    )
                }
            }
            IconButton(modifier = Modifier.size(48.dp), onClick = onSeekBackward) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(id = R.drawable.replay_10),
                    contentDescription = stringResource(R.string.rewind_10_seconds),
                    tint = Color.White
                )
            }
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = if (playbackState == PlaybackState.PLAYING) onPause else onPlay
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(if (playbackState == PlaybackState.PLAYING) R.drawable.pause else R.drawable.play),
                    contentDescription = stringResource(id = R.string.play),
                    tint = Color.White
                )
            }
            IconButton(modifier = Modifier.size(48.dp), onClick = onSeekForward) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(id = R.drawable.forward_10),
                    contentDescription = stringResource(id = R.string.forward_10_seconds),
                    tint = Color.White
                )
            }
            AnimatedVisibility(playlist.lastOrNull()?.id != currentlyPlayingStory.id) {
                IconButton(onClick = onNext) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        painter = painterResource(id = R.drawable.next),
                        contentDescription = stringResource(R.string.next),
                        tint = Color.White
                    )
                }
            }
        }
        val contentPadding = 16
        val numberOfItems = when (LocalConfiguration.current.screenWidthDp.dp.fromWidth()) {
            WindowWidthSizeClass.Compact -> 2
            WindowWidthSizeClass.Medium -> 3
            else -> 4
        }
        val peekSize = 32
        val listState = rememberLazyListState()
        val itemWidth =
            (LocalConfiguration.current.screenWidthDp - (contentPadding * (numberOfItems + 1)) - peekSize) / numberOfItems
        if (playlist.size > 1) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(contentPadding.dp)
            ) {
                items(playlist) {
                    PlaylistItem(
                        it,
                        selected = currentlyPlayingStory.id == it.id,
                        modifier = Modifier
                            .padding(end = contentPadding.dp)
                            .width(itemWidth.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistItem(story: Story, selected: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Image(painter = story.imageUri?.let { Uri.parse(it) }?.path?.let {
            try {
                BitmapPainter(BitmapFactory.decodeFile(it).asImageBitmap())
            } catch (e: Exception) {
                null
            }
        } ?: painterResource(id = R.drawable.book),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp))
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = story.voicedBy,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun PlaylistItem_Preview() {
    PillowBargeTheme {
        Row {
            PlaylistItem(
                story = Story(
                    0,
                    title = "reprehendunt",
                    voicedBy = "moderatius",
                    type = StoryType.Story,
                    imageUri = null,
                    audioUri = "malesuada"
                ),
                selected = false
            )
        }
    }
}

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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun Player_Preview() {
    PillowBargeTheme {
        Player(
            currentlyPlayingStory = Story(
                3287,
                "Mamma Mu får ett sår",
                "Pappa",
                StoryType.Story,
                "",
                ""
            ),
            playlist = listOf(
                Story(
                    id = 3286,
                    title = "taciti taciti taciti, taciti taciti",
                    voicedBy = "habeo habeo habeo habeo habeo",
                    type = StoryType.Story,
                    imageUri = null,
                    audioUri = "audire"
                ),
                Story(
                    id = 3287,
                    title = "taciti",
                    voicedBy = "habeo",
                    type = StoryType.Story,
                    imageUri = null,
                    audioUri = "audire"
                ),
                Story(
                    id = 3288,
                    title = "taciti",
                    voicedBy = "habeo",
                    type = StoryType.Story,
                    imageUri = null,
                    audioUri = "audire"
                )
            ),
            onPrevious = {},
            onSeekBackward = {},
            onPlay = {},
            onPause = {},
            onSeekForward = {},
            onNext = {},
            onClose = {},
            playbackState = PlaybackState.PLAYING
        )
    }
}
