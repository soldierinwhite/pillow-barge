package io.soldierinwhite.pillowbarge.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryDao
import io.soldierinwhite.pillowbarge.player.PlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storyDao: StoryDao,
    private val playbackController: PlaybackController
) : ViewModel() {
    val homeUiState = combine(
        playbackController.playbackState, storyDao.getAllFlow()
    ) { playbackState, stories ->
        HomeUiState(playbackState, stories)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState(PlaybackState.UNINITIALISED, listOf())
    )

    fun startAudio(audioUriString: String) {
        playbackController.sendPlayerEvent(
            PlaybackController.PlayerEvent.Start(
                MediaItem.fromUri(
                    audioUriString
                )
            )
        )
    }

    fun seekBack() {
        playbackController.sendPlayerEvent(PlaybackController.PlayerEvent.SeekBackward(seekIncrement))
    }

    fun seekForward() {
        playbackController.sendPlayerEvent(PlaybackController.PlayerEvent.SeekForward(seekIncrement))
    }

    fun pause() {
        playbackController.sendPlayerEvent(PlaybackController.PlayerEvent.Pause)
    }

    fun play() {
        playbackController.sendPlayerEvent(PlaybackController.PlayerEvent.Play)
    }

    fun delete(story: Story) {
        viewModelScope.launch(Dispatchers.IO) { storyDao.delete(story) }
        Uri.parse(story.imageUri).path?.let { File(it).delete() }
        Uri.parse(story.audioUri).path?.let { File(it).delete() }
    }

    fun stop() {
        playbackController.sendPlayerEvent(PlaybackController.PlayerEvent.Release)
    }

    override fun onCleared() {
        stop()
    }

    fun addToQueue(audioUriString: String) {
        playbackController.sendPlayerEvent(
            PlaybackController.PlayerEvent.Queue(
                MediaItem.fromUri(
                    audioUriString
                )
            )
        )
    }

    companion object {
        private val seekIncrement = Duration.ofSeconds(10)
    }
}

data class HomeUiState(
    val playbackState: PlaybackState,
    val stories: List<Story>
)

enum class PlaybackState {
    PLAYING,
    PAUSED,
    STOPPED,
    UNINITIALISED
}
