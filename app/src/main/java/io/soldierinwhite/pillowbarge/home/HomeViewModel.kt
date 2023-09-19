package io.soldierinwhite.pillowbarge.home

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryDao
import io.soldierinwhite.pillowbarge.player.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storyDao: StoryDao,
    private val application: Application
) : ViewModel() {

    private var controller: MediaController? = null
    private val currentMediaItem = MutableStateFlow<MediaItem?>(null)
    private val playbackState = MutableStateFlow(PlaybackState.STOPPED)

    @OptIn(ExperimentalCoroutinesApi::class)
    val homeUiState = combine(
        currentMediaItem.mapLatest { it }.distinctUntilChanged()
            .flatMapLatest { mediaItem ->
                if (mediaItem != null) {
                    val sessionToken =
                        SessionToken(
                            application.applicationContext,
                            ComponentName(
                                application.applicationContext,
                                PlaybackService::class.java
                            )
                        )
                    val controllerFuture =
                        MediaController.Builder(application.applicationContext, sessionToken)
                            .buildAsync()
                    controllerFuture.addListener({
                        controller = controllerFuture.get().also {
                            it.setMediaItem(mediaItem)
                            it.prepare()
                            it.play()
                            it.addListener(object : Player.Listener {
                                override fun onIsPlayingChanged(playing: Boolean) {
                                    playbackState.value =
                                        if (playing) PlaybackState.PLAYING else PlaybackState.PAUSED
                                }

                                override fun onPlaybackStateChanged(ps: Int) {
                                    if (ps in setOf(STATE_ENDED, STATE_IDLE)) {
                                        playbackState.value = PlaybackState.STOPPED
                                        currentMediaItem.value = null
                                        MediaController.releaseFuture(controllerFuture)
                                    }
                                }
                            }.also { listener -> listeners.add(listener) })
                        }
                    }, MoreExecutors.directExecutor())
                } else {
                    playbackState.value = PlaybackState.STOPPED
                }
                playbackState
            }, storyDao.getAllFlow()
    ) { p, stories ->
        HomeUiState(p, stories)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState(PlaybackState.STOPPED, listOf())
    )

    private val listeners = mutableListOf<Player.Listener>()

    fun startAudio(audioUriString: String) {
        currentMediaItem.value = MediaItem.fromUri(audioUriString)
    }

    fun seekBack() {
        controller?.run {
            seekTo(currentPosition - SEEK_INCREMENT)
        }
    }

    fun seekForward() {
        controller?.run {
            seekTo(currentPosition + SEEK_INCREMENT)
        }
    }

    fun stop() {
        controller?.run {
            stop()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun play() {
        controller?.play()
    }

    fun delete(story: Story) {
        viewModelScope.launch(Dispatchers.IO) { storyDao.delete(story) }
        Uri.parse(story.imageUri).path?.let { File(it).delete() }
        Uri.parse(story.audioUri).path?.let { File(it).delete() }
    }

    private fun release() {
        controller?.run {
            stop()
            listeners.forEach { removeListener(it) }
            release()
        }
    }

    override fun onCleared() {
        release()
    }

    fun addToQueue(audioUriString: String) {
        controller?.addMediaItem(MediaItem.fromUri(audioUriString))
    }
}

data class HomeUiState(
    val playbackState: PlaybackState,
    val stories: List<Story>
)

enum class PlaybackState {
    PLAYING,
    PAUSED,
    STOPPED
}

private const val SEEK_INCREMENT = 10000L
