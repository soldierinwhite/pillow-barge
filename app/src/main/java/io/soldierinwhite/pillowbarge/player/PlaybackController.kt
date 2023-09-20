package io.soldierinwhite.pillowbarge.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import io.soldierinwhite.pillowbarge.home.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    private val application: Application
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var controller: MediaController? = null
    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    private val currentMediaItem: Flow<MediaItem?> get() = _currentMediaItem
    private val listeners = mutableListOf<Player.Listener>()

    private var releaseMediaService: () -> Unit = {}

    @OptIn(ExperimentalCoroutinesApi::class)
    val playbackState =
        currentMediaItem.distinctUntilChanged().transformLatest { mediaItem ->
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
                releaseMediaService = { MediaController.releaseFuture(controllerFuture) }
                controllerFuture.addListener({
                    controller = controllerFuture.get().also {
                        it.setMediaItem(mediaItem)
                        it.prepare()
                        it.play()
                        it.addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(playing: Boolean) {
                                scope.launch { emit(if (playing) PlaybackState.PLAYING else PlaybackState.PAUSED) }
                            }

                            override fun onPlaybackStateChanged(ps: Int) {
                                if (ps in setOf(Player.STATE_ENDED, Player.STATE_IDLE)) {
                                    scope.launch { emit(PlaybackState.STOPPED) }
                                }
                            }
                        }.also { listener -> listeners.add(listener) })
                    }
                }, MoreExecutors.directExecutor())
            } else {
                scope.launch { emit(PlaybackState.UNINITIALISED) }
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), PlaybackState.UNINITIALISED)

    fun sendPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            PlayerEvent.Pause -> controller?.pause()
            PlayerEvent.Play -> controller?.play()
            PlayerEvent.Release -> {
                controller?.run {
                    listeners.forEach { removeListener(it) }
                    stop()
                    release()
                    controller = null
                    releaseMediaService()
                    _currentMediaItem.value = null
                    application.stopService(Intent(application, PlaybackService::class.java))
                }
            }

            is PlayerEvent.SeekBackward -> {
                controller?.run {
                    seekTo(currentPosition - playerEvent.duration.toMillis())
                }
            }

            is PlayerEvent.SeekForward -> {
                controller?.run {
                    seekTo(currentPosition + playerEvent.duration.toMillis())
                }
            }

            is PlayerEvent.Start -> {
                _currentMediaItem.value = playerEvent.mediaItem
            }

            is PlayerEvent.Queue -> {
                controller?.addMediaItem(playerEvent.mediaItem)
            }
        }
    }

    sealed class PlayerEvent {
        data class Start(val mediaItem: MediaItem) : PlayerEvent()
        object Pause : PlayerEvent()
        object Play : PlayerEvent()
        data class SeekForward(val duration: Duration) : PlayerEvent()
        data class SeekBackward(val duration: Duration) : PlayerEvent()
        object Release : PlayerEvent()
        data class Queue(val mediaItem: MediaItem) : PlayerEvent()
    }

}
