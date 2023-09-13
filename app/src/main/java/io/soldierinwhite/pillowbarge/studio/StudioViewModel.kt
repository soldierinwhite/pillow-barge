package io.soldierinwhite.pillowbarge.studio

import android.app.Application
import android.media.MediaRecorder
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StudioViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {
    private var audioFilePath: String = ""
    private var recorder: MediaRecorder? = null
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> get() = _isRecording

    private val _canSave = MutableStateFlow(false)
    val canSave: StateFlow<Boolean> get() = _canSave

    fun record() {
        audioFilePath = "${application.filesDir.absoluteFile}/${UUID.randomUUID()}.mp4".also {
            File(it).createNewFile()
        }
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFilePath)
            setMaxFileSize(50 * 1000 * 1000)
            prepare()
            start()
        }
        _isRecording.value = true
    }

    fun stop() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        _canSave.value = true
        _isRecording.value = false
    }

    fun save(): Pair<String?, Uri?> = Uri.fromFile(File(audioFilePath))?.let { uri ->
        (uri.lastPathSegment ?: "Recorded audio") to uri
    } ?: (null to null)

    fun discard() {
        _canSave.value = false
        audioFilePath.takeIf { it.isNotEmpty() }?.let { File(it).delete() }
    }

    override fun onCleared() {
        recorder?.release()
        recorder = null
        audioFilePath.takeIf { it.isNotEmpty() }?.let { File(it) }?.run {
            if (exists()) {
                deleteRecursively()
            }
        }
    }
}
