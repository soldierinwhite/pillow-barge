package io.soldierinwhite.pillowbarge.home

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.soldierinwhite.pillowbarge.R
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryCard
import io.soldierinwhite.pillowbarge.player.PlaybackButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Home(
    viewModel: HomeViewModel = hiltViewModel(),
    windowWidthSizeClass: WindowWidthSizeClass,
    onFabClick: () -> Unit
) {
    var showPlayerUI by rememberSaveable {
        mutableStateOf(false)
    }

    val stories by viewModel.stories.collectAsState(initial = listOf())
    Log.d("stories", stories.size.toString())
    Scaffold(
        modifier = Modifier.animateContentSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showPlayerUI,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                val playing by viewModel.isPlaying
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.Center
                ) {
                    PlaybackButton(
                        onClick = { viewModel.seekBack() },
                        icon = R.drawable.replay_10,
                        modifier = Modifier.weight(1f),
                        contentDescription = stringResource(R.string.rewind_10_seconds)
                    )
                    PlaybackButton(
                        onClick = { if (playing) viewModel.pause() else viewModel.play() },
                        icon = if (playing) R.drawable.pause else R.drawable.play,
                        modifier = Modifier.weight(1f),
                        contentDescription = if (playing) stringResource(R.string.pause) else stringResource(
                            R.string.play
                        )
                    )
                    PlaybackButton(
                        onClick = { viewModel.seekForward() },
                        icon = R.drawable.forward_10,
                        modifier = Modifier.weight(1f),
                        contentDescription = stringResource(R.string.forward_10_seconds)
                    )
                    PlaybackButton(
                        onClick = { viewModel.stop() },
                        icon = R.drawable.stop,
                        modifier = Modifier.weight(1f),
                        contentDescription = stringResource(R.string.stop)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onFabClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_story)
                )
            }
        }
    ) { paddingValues: PaddingValues ->
        Surface(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var showDeleteDialogStory: Story? by remember { mutableStateOf(null) }
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(
                    when (windowWidthSizeClass) {
                        WindowWidthSizeClass.Compact -> 2
                        WindowWidthSizeClass.Medium -> 3
                        else -> 4
                    }
                ),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(stories) {
                    StoryCard(
                        story = it,
                        onStoryClick = { story ->
                            showPlayerUI = true
                            viewModel.startAudio(story.audioUri) {
                                showPlayerUI = false
                            }
                        },
                        onDeleteClick = { showDeleteDialogStory = it },
                        modifier = Modifier
                            .animateItemPlacement()
                            .padding(4.dp)
                    )
                }
            }
            AnimatedVisibility(visible = showDeleteDialogStory != null) {
                AlertDialog(
                    title = {
                        Text(
                            stringResource(R.string.delete_story),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = { Text(stringResource(R.string.delete_story_text)) },
                    confirmButton = {
                        IconButton(onClick = {
                            showDeleteDialogStory?.let { viewModel.delete(it) }
                            showDeleteDialogStory = null
                        }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.confirm)
                            )
                        }
                    },
                    onDismissRequest = { showDeleteDialogStory = null })
            }
        }
    }
}
