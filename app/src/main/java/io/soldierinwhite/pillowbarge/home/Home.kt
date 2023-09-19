package io.soldierinwhite.pillowbarge.home

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.soldierinwhite.pillowbarge.R
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryCard
import io.soldierinwhite.pillowbarge.model.story.StoryType
import io.soldierinwhite.pillowbarge.player.PlaybackButton
import io.soldierinwhite.pillowbarge.ui.theme.PillowBargeTheme
import io.soldierinwhite.pillowbarge.util.fromWidth

@Composable
fun Home(
    viewModel: HomeViewModel = hiltViewModel(),
    onFabClick: () -> Unit
) {
    val windowWidthSizeClass = LocalConfiguration.current.screenWidthDp.dp.fromWidth()
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    Home(
        stories = uiState.stories,
        playbackState = uiState.playbackState,
        windowWidthSizeClass = windowWidthSizeClass,
        onPause = { viewModel.pause() },
        onPlay = { viewModel.play() },
        onSeekBack = { viewModel.seekBack() },
        onSeekForward = { viewModel.seekForward() },
        onStop = { viewModel.stop() },
        onFabClick = onFabClick,
        onStartItem = { audioUri -> viewModel.startAudio(audioUri) },
        onQueueItem = { viewModel.addToQueue(it) },
        onDelete = { viewModel.delete(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Home(
    stories: List<Story>,
    playbackState: PlaybackState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onStop: () -> Unit,
    onFabClick: () -> Unit,
    onStartItem: (String) -> Unit,
    onQueueItem: (String) -> Unit,
    onDelete: (Story) -> Unit
) {
    val showPlayerUI = playbackState != PlaybackState.STOPPED
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val isPlaying = playbackState == PlaybackState.PLAYING
    Scaffold(
        modifier = Modifier
            .animateContentSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showPlayerUI,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.Center
                ) {
                    PlaybackButton(
                        onClick = onSeekBack,
                        icon = R.drawable.replay_10,
                        modifier = Modifier.weight(1f),
                        contentDescription = stringResource(R.string.rewind_10_seconds)
                    )
                    PlaybackButton(
                        onClick = { if (isPlaying) onPause() else onPlay() },
                        icon = if (isPlaying) R.drawable.pause else R.drawable.play,
                        modifier = Modifier.weight(1f),
                        contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(
                            R.string.play
                        )
                    )
                    PlaybackButton(
                        onClick = onSeekForward,
                        icon = R.drawable.forward_10,
                        modifier = Modifier.weight(1f),
                        contentDescription = stringResource(R.string.forward_10_seconds)
                    )
                    PlaybackButton(
                        onClick = onStop,
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
                contentPadding = PaddingValues(8.dp),
            ) {
                items(stories) {
                    StoryCard(
                        story = it,
                        onStoryClick = { story ->
                            onStartItem(story.audioUri)
                        },
                        onDeleteClick = { showDeleteDialogStory = it },
                        onEnqueue = { onQueueItem(it.audioUri) },
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
                            showDeleteDialogStory?.let { onDelete(it) }
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

@Preview
@Composable
fun Home_Preview() {
    PillowBargeTheme {
        Home(
            stories = listOf(
                Story(
                    id = 7606,
                    title = "inciderint",
                    voicedBy = "justo",
                    type = StoryType.Story,
                    imageUri = null,
                    audioUri = "a"
                ),
                Story(
                    id = 7607,
                    title = "inciderint",
                    voicedBy = "justo",
                    type = StoryType.Story,
                    imageUri = null,
                    audioUri = "a"
                ),
                Story(
                    id = 7608,
                    title = "inciderint",
                    voicedBy = "justo",
                    type = StoryType.Story,
                    imageUri = null,
                    audioUri = "a"
                )
            ),
            playbackState = PlaybackState.PLAYING,
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onPause = { },
            onPlay = { },
            onSeekBack = { },
            onSeekForward = { },
            onStop = { },
            onFabClick = { },
            onStartItem = { },
            onQueueItem = {},
            onDelete = {}
        )
    }
}
