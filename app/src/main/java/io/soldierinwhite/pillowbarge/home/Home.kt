package io.soldierinwhite.pillowbarge.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.soldierinwhite.pillowbarge.R
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryCard
import io.soldierinwhite.pillowbarge.model.story.StoryType
import io.soldierinwhite.pillowbarge.player.Player
import io.soldierinwhite.pillowbarge.ui.theme.PillowBargeTheme
import io.soldierinwhite.pillowbarge.util.fromWidth
import kotlinx.coroutines.launch

@Composable
fun Home(
    viewModel: HomeViewModel = hiltViewModel(), onFabClick: () -> Unit
) {
    val windowWidthSizeClass = LocalConfiguration.current.screenWidthDp.dp.fromWidth()
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    Home(stories = uiState.stories,
        currentlyPlayingStory = uiState.currentlyPlaylingStory,
        playlist = uiState.playlist,
        playbackState = uiState.playbackState,
        windowWidthSizeClass = windowWidthSizeClass,
        onPause = { viewModel.pause() },
        onPlay = { viewModel.play() },
        onSeekBack = { viewModel.seekBack() },
        onSeekForward = { viewModel.seekForward() },
        onNext = { viewModel.next() },
        onPrevious = { viewModel.previous() },
        onStop = { viewModel.stop() },
        onFabClick = onFabClick,
        onStartItem = { audioUri -> viewModel.startAudio(audioUri) },
        onQueueItem = { viewModel.addToQueue(it) },
        onDelete = { viewModel.delete(it) })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Home(
    stories: List<Story>,
    currentlyPlayingStory: Story?,
    playlist: List<Story>,
    playbackState: PlaybackState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onFabClick: () -> Unit,
    onStartItem: (String) -> Unit,
    onQueueItem: (String) -> Unit,
    onDelete: (Story) -> Unit
) {
    val showPlayerUI = playbackState != PlaybackState.UNINITIALISED || currentlyPlayingStory != null
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val isPlaying = playbackState == PlaybackState.PLAYING
    val peekHeight by animateDpAsState(
        label = "fabOffset",
        targetValue = if (showPlayerUI) 64.dp else 0.dp
    )
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    BottomSheetScaffold(modifier = Modifier
        .animateContentSize()
        .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                scrollBehavior = scrollBehavior
            )
        },
        scaffoldState = bottomSheetScaffoldState,
        sheetDragHandle = {},
        containerColor = Color.Transparent,
        sheetPeekHeight = peekHeight,
        sheetContent = {
            val scope = rememberCoroutineScope()
            var collapseMiniPlayer by rememberSaveable { mutableStateOf(false) }
            if (!collapseMiniPlayer && currentlyPlayingStory != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable {
                            collapseMiniPlayer = true
                            scope.launch { bottomSheetScaffoldState.bottomSheetState.expand() }
                        }
                ) {
                    Image(painter = currentlyPlayingStory.imageUri?.let { Uri.parse(it) }?.path?.let {
                        try {
                            BitmapPainter(BitmapFactory.decodeFile(it).asImageBitmap())
                        } catch (e: Exception) {
                            null
                        }
                    } ?: painterResource(id = R.drawable.book),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp)
                    )
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = currentlyPlayingStory.title,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentlyPlayingStory.voicedBy,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        modifier = Modifier
                            .align(CenterVertically)
                            .size(48.dp),
                        onClick = { if (isPlaying) onPause() else onPlay() }) {
                        Icon(
                            painterResource(id = if (isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play)
                        )
                    }
                    IconButton(
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(end = 16.dp)
                            .size(48.dp),
                        onClick = {
                            onStop()
                        }) {
                        Icon(
                            painterResource(id = R.drawable.stop),
                            contentDescription = stringResource(R.string.stop)
                        )
                    }
                }
            }
            currentlyPlayingStory?.let {
                Player(
                    modifier = Modifier.weight(1f),
                    currentlyPlayingStory = it,
                    playlist = playlist,
                    playbackState = playbackState,
                    onPrevious = onPrevious,
                    onSeekBackward = onSeekBack,
                    onPlay = onPlay,
                    onPause = onPause,
                    onSeekForward = onSeekForward,
                    onNext = onNext,
                    onClose = {
                        collapseMiniPlayer = false
                        scope.launch { bottomSheetScaffoldState.bottomSheetState.partialExpand() }
                    },
                )
            }
        }) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var showDeleteDialogStory: Story? by remember { mutableStateOf(null) }
            Box(modifier = Modifier.fillMaxHeight()) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(
                        when (windowWidthSizeClass) {
                            WindowWidthSizeClass.Compact -> 2
                            WindowWidthSizeClass.Medium -> 3
                            else -> 4
                        }
                    ),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = peekHeight + 8.dp
                    ),
                ) {
                    items(stories) {
                        StoryCard(
                            story = it,
                            onStoryClick = { story ->
                                onStartItem(story.audioUri)
                            },
                            onDeleteClick = { showDeleteDialogStory = it },
                            onEnqueue = {
                                onQueueItem(it.audioUri)
                            },
                            modifier = Modifier
                                .animateItemPlacement()
                                .padding(4.dp)
                        )
                    }
                }
                FloatingActionButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(48.dp)
                        .align(Alignment.BottomEnd),
                    onClick = onFabClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_story)
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun Home_Preview() {
    PillowBargeTheme {
        Home(stories = listOf(
            Story(
                id = 7606,
                title = "inciderint",
                voicedBy = "justo",
                type = StoryType.Story,
                imageUri = null,
                audioUri = "a"
            ), Story(
                id = 7607,
                title = "inciderint",
                voicedBy = "justo",
                type = StoryType.Story,
                imageUri = null,
                audioUri = "a"
            ), Story(
                id = 7608,
                title = "inciderint",
                voicedBy = "justo",
                type = StoryType.Story,
                imageUri = null,
                audioUri = "a"
            )
        ),
            currentlyPlayingStory = Story(
                id = 7607,
                title = "inciderint",
                voicedBy = "justo",
                type = StoryType.Story,
                imageUri = null,
                audioUri = "a"
            ),
            playlist = listOf(
                Story(
                    id = 7607,
                    title = "inciderint",
                    voicedBy = "justo",
                    type = StoryType.Story,
                    imageUri = null,
                    audioUri = "a"
                ), Story(
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
            onNext = { },
            onPrevious = { },
            onStop = { },
            onFabClick = { },
            onStartItem = { },
            onQueueItem = {},
            onDelete = {})
    }
}
