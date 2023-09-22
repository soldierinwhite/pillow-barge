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
import kotlinx.coroutines.flow.combine
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
    private val uriList = MutableStateFlow<List<MediaItem>>(listOf())
    private val listeners = mutableListOf<Player.Listener>()

    private var releaseMediaService: () -> Unit = {}

    @OptIn(ExperimentalCoroutinesApi::class)
    val playbackDetails = combine(
        uriList,
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
                        uriList.value = listOf(mediaItem)
                        it.prepare()
                        it.play()
                        it.addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(playing: Boolean) {
                                scope.launch { emit(mediaItem to if (playing) PlaybackState.PLAYING else PlaybackState.PAUSED) }
                            }

                            override fun onPlaybackStateChanged(ps: Int) {
                                if (ps in setOf(Player.STATE_ENDED, Player.STATE_IDLE)) {
                                    scope.launch { emit(mediaItem to PlaybackState.STOPPED) }
                                }
                            }
                        }.also { listener -> listeners.add(listener) })
                    }
                }, MoreExecutors.directExecutor())
            } else {
                scope.launch { emit(null to PlaybackState.UNINITIALISED) }
            }
        }) { uris, (currentUri, state) ->
        PlaybackDetails(
            currentlyPlayingMediaItem = currentUri,
            mediaItems = uris,
            playbackState = state
        )
    }.distinctUntilChanged().stateIn(
        scope,
        SharingStarted.WhileSubscribed(5000),
        PlaybackDetails(null, listOf(), PlaybackState.UNINITIALISED)
    )

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
                uriList.value = uriList.value + listOf(playerEvent.mediaItem)
                controller?.addMediaItem(playerEvent.mediaItem)
            }

            PlayerEvent.Next -> {
                controller?.seekToNextMediaItem()
            }

            PlayerEvent.Previous -> {
                controller?.seekToPreviousMediaItem()
            }
        }
    }

    data class PlaybackDetails(
        val currentlyPlayingMediaItem: MediaItem?,
        val mediaItems: List<MediaItem>,
        val playbackState: PlaybackState
    )

    sealed class PlayerEvent {
        data class Start(val mediaItem: MediaItem) : PlayerEvent()
        object Pause : PlayerEvent()
        object Play : PlayerEvent()
        data class SeekForward(val duration: Duration) : PlayerEvent()
        data class SeekBackward(val duration: Duration) : PlayerEvent()
        object Release : PlayerEvent()
        data class Queue(val mediaItem: MediaItem) : PlayerEvent()

        object Next : PlayerEvent()
        object Previous : PlayerEvent()
    }

}
