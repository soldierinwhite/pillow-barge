package io.soldierinwhite.pillowbarge.addstory

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddStoryViewModel @Inject constructor(
    application: Application,
    private val storyDao: StoryDao
) : ViewModel() {
    private val contentResolver = application.applicationContext.contentResolver
    private val filesDirectory = application.applicationContext.filesDir

    private val story: MutableStateFlow<Story?> = MutableStateFlow(null)
    private val audioUri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    private val _addStoryUIState = MutableStateFlow(AddStoryUIState(null, null))
    val addStoryUIState: StateFlow<AddStoryUIState> get() = _addStoryUIState

    fun onImageUri(uri: Uri?) {
        audioUri.value?.let {
            viewModelScope.launch {
                inputStreamToFileUriAndName(uri)?.let { (uri, name) ->
                    _addStoryUIState.emit(addStoryUIState.value.copy(imageFilename = name))
                    story.emit(Story(audioUri = it.toString(), imageUri = uri.toString()))
                }
            }
        }
    }

    fun onAudioUri(uri: Uri) {
        viewModelScope.launch {
            inputStreamToFileUriAndName(uri)?.let { (uri, name) ->
                audioUri.emit(uri)
                _addStoryUIState.emit(addStoryUIState.value.copy(audioFilename = name))
            }
        }
    }

    private fun inputStreamToFileUriAndName(uri: Uri?): Pair<Uri, String>? {
        if (uri == null) return null
        val type = contentResolver.getType(uri)?.substringAfter('/')
        return type?.let {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null
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
        story.value?.let {
            viewModelScope.launch(Dispatchers.IO) {
                storyDao.insert(it)
            }
        }
    }

    data class AddStoryUIState(
        val audioFilename: String?,
        val imageFilename: String?
    )
}
