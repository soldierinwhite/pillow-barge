package io.soldierinwhite.pillowbarge.addstory

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryDao
import io.soldierinwhite.pillowbarge.model.story.StoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val titleState = MutableStateFlow("")
    private val voicedByState = MutableStateFlow("")
    private val typeState = MutableStateFlow(StoryType.Story)

    private lateinit var imageFile: File

    val addStoryUIState = combine(
        titleState,
        voicedByState,
        typeState,
        savedStateHandle.getStateFlow<String?>(AUDIO_FILENAME_KEY, null),
        savedStateHandle.getStateFlow<String?>(IMAGE_FILENAME_KEY, null)
    ) { title, voicedBy, type, audioFilename, imageFilename ->
        AddStoryUIState(
            title = title,
            voicedBy = voicedBy,
            type = type,
            audioFilename = audioFilename,
            imageFilename = imageFilename
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AddStoryUIState()
    )

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

    fun setTitle(title: String) {
        titleState.value = title
    }

    fun setVoicedBy(voicedBy: String) {
        voicedByState.value = voicedBy
    }

    fun setType(type: StoryType) {
        typeState.value = type
    }

    fun onPhotoResult() {
        Uri.fromFile(imageFile).let {
            if (it.toString().isNotBlank()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(
                                contentResolver,
                                it
                            )
                        ) { decoder, _, _ ->
                            decoder.setTargetSampleSize(1)
                            decoder.isMutableRequired = true
                        }
                    } else {
                        MediaStore.Images.Media.getBitmap(contentResolver, it)
                    }
                    val filePath = "${filesDirectory.absolutePath}/${UUID.randomUUID()}.jpeg"
                    val file = File(filePath)
                    val fileOutputStream = File(filePath).outputStream()
                    try {
                        bitmap?.compress(Bitmap.CompressFormat.JPEG, 60, fileOutputStream)
                        fileOutputStream.flush()
                        fileOutputStream.close()
                        onImageUri(Uri.fromFile(file))
                    } catch (e: Exception) {
                        Log.d("Image intent result", "Failed")
                        //fail silently
                    }
                }
            }
        }
    }

    fun setImageFile(file: File) {
        imageFile = file
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
                        title = addStoryUIState.value.title.trim(),
                        voicedBy = addStoryUIState.value.voicedBy.trim(),
                        type = addStoryUIState.value.type,
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
        val title: String = "",
        val voicedBy: String = "",
        val type: StoryType = StoryType.Story,
        val audioFilename: String? = null,
        val imageFilename: String? = null
    ) {
        fun isValid() = title.isNotEmpty() && voicedBy.isNotEmpty() && audioFilename.isNullOrBlank()
            .not() && imageFilename.isNullOrBlank().not()
    }

    companion object {
        const val AUDIO_URI_KEY = "audioUri"
        const val AUDIO_FILENAME_KEY = "audioFilename"
        const val IMAGE_URI_KEY = "imageUri"
        const val IMAGE_FILENAME_KEY = "imageFilename"
    }
}
