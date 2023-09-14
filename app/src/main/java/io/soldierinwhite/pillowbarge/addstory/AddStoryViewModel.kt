package io.soldierinwhite.pillowbarge.addstory

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.soldierinwhite.pillowbarge.extensions.parcelable
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddStoryViewModel @Inject constructor(
    application: Application,
    private val storyDao: StoryDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val contentResolver = application.applicationContext.contentResolver
    private val filesDirectory = application.applicationContext.filesDir

    val addStoryUIState = combine(
        savedStateHandle.getStateFlow<String?>(AUDIO_FILENAME_KEY, null),
        savedStateHandle.getStateFlow<String?>(IMAGE_FILENAME_KEY, null)
    ) { audioFilename, imageFilename ->
        AddStoryUIState(audioFilename, imageFilename)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AddStoryUIState(null, null))

    fun onImageUri(uri: Uri?) {
        viewModelScope.launch {
            when (uri?.scheme) {
                "file" -> {
                    savedStateHandle[IMAGE_URI_KEY] = uri.toString()
                    savedStateHandle[IMAGE_FILENAME_KEY] = uri.lastPathSegment
                }

                "content" -> {
                    inputStreamToFileUriAndName(uri)?.let { (fileUri, name) ->
                        savedStateHandle[IMAGE_URI_KEY] = fileUri.toString()
                        savedStateHandle[IMAGE_FILENAME_KEY] = name
                    }
                }
            }
        }
    }


    fun onAudioUri(uri: Uri) {
        viewModelScope.launch {
            inputStreamToFileUriAndName(uri)?.let { (uri, name) ->
                savedStateHandle[AUDIO_URI_KEY] = uri.toString()
                savedStateHandle[AUDIO_FILENAME_KEY] = name
            }
        }
    }

    fun onPhotoResult(activityResult: ActivityResult) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = activityResult.data?.extras?.parcelable<Bitmap>("data")
            val filePath = "${filesDirectory.absolutePath}/${UUID.randomUUID()}.jpeg"
            val file = File(filePath)
            val fileOutputStream = File(filePath).outputStream()
            try {
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
                onImageUri(Uri.fromFile(file))
            } catch (e: Exception) {
                Log.d("Image intent result", "Failed")
                //fail silently
            }
        }
    }

    private fun inputStreamToFileUriAndName(uri: Uri?): Pair<Uri, String>? {
        if (uri == null) return null
        val type = contentResolver.getType(uri)?.substringAfter('/')
        return type?.let {
            contentResolver.query(
                uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                cursor.moveToFirst()
                cursor.getString(0)
            }?.let { name ->
                contentResolver.openInputStream(uri).use { inputStream ->
                    val file = File("${filesDirectory.absoluteFile}/${UUID.randomUUID()}.$type")
                    val output = FileOutputStream(file)
                    inputStream?.copyTo(output, 4 * 1024)
                    Uri.fromFile(file) to name
                }
            }
        }
    }

    fun addStory() {
        viewModelScope.launch(Dispatchers.IO) {
            savedStateHandle.get<String?>(AUDIO_URI_KEY)?.let { audioUriString ->
                storyDao.insert(
                    Story(
                        audioUri = audioUriString,
                        imageUri = savedStateHandle[IMAGE_URI_KEY]
                    )
                )
            }
        }
    }

    fun updateSaveState(savedStateHandle: SavedStateHandle) {
        this.savedStateHandle.run {
            savedStateHandle.get<String?>(AUDIO_URI_KEY)?.let { set(AUDIO_URI_KEY, it) }
            savedStateHandle.get<String?>(IMAGE_URI_KEY)?.let { set(IMAGE_URI_KEY, it) }
            savedStateHandle.get<String?>(AUDIO_FILENAME_KEY)?.let { set(AUDIO_FILENAME_KEY, it) }
            savedStateHandle.get<String?>(IMAGE_FILENAME_KEY)?.let { set(IMAGE_FILENAME_KEY, it) }
        }
    }

    data class AddStoryUIState(
        val audioFilename: String?, val imageFilename: String?
    )

    companion object {
        const val AUDIO_URI_KEY = "audioUri"
        const val AUDIO_FILENAME_KEY = "audioFilename"
        const val IMAGE_URI_KEY = "imageUri"
        const val IMAGE_FILENAME_KEY = "imageFilename"
    }
}
