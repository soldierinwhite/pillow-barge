package io.soldierinwhite.pillowbarge.home

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val storyDao: StoryDao
) : ViewModel() {
    private var sessionToken =
        application.applicationContext.let {
            SessionToken(
                it,
                ComponentName(it, PlaybackService::class.java)
            )
        }

    private val controllerFuture =
        MediaController.Builder(application.applicationContext, sessionToken).buildAsync()

    private var controller: MediaController? = null

    init {
        controllerFuture.addListener({
            controller = controllerFuture.get()
        }, MoreExecutors.directExecutor())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val stories = storyDao.getAllFlow().mapLatest { stories ->
        stories.filter { story ->
            Uri.parse(story.audioUri).path?.let { !File(it).exists() } ?: false
        }.toTypedArray().takeIf { it.isNotEmpty() }?.let { missingStories ->
            viewModelScope.launch(Dispatchers.IO) { storyDao.delete(*missingStories) }
        }
        stories.filter { story ->
            story.imageUri?.let { Uri.parse(it).path?.let { path -> !File(path).exists() } }
                ?: false
        }.toTypedArray().takeIf { it.isNotEmpty() }?.let { missingImages ->
            viewModelScope.launch(Dispatchers.IO) {
                storyDao.update(*missingImages.map {
                    it.copy(
                        imageUri = null
                    )
                }.toTypedArray())
            }
        }
        stories
    }

    private val _isPlaying: MutableState<Boolean> = mutableStateOf(controller?.isPlaying == true)
    val isPlaying: State<Boolean> get() = _isPlaying

    private val listeners = mutableListOf<Player.Listener>()

    fun startAudio(audioUriString: String, onEnded: () -> Unit) {
        controller?.run {
            setMediaItem(MediaItem.fromUri(audioUriString))
            prepare()
            play()
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState in setOf(STATE_ENDED, STATE_IDLE)) {
                        onEnded()
                    }
                }
            }.also { listeners.add(it) })
        }
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

    fun addToQueue(it: Story) {
        controller?.addMediaItem(MediaItem.fromUri(it.audioUri))
    }
}

private const val SEEK_INCREMENT = 10000L
