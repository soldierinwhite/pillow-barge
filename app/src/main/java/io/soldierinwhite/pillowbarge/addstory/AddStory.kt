package io.soldierinwhite.pillowbarge.addstory

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import io.soldierinwhite.pillowbarge.R
import io.soldierinwhite.pillowbarge.addstory.AddStoryViewModel.AddStoryUIState
import io.soldierinwhite.pillowbarge.model.story.StoryType
import io.soldierinwhite.pillowbarge.ui.theme.PillowBargeTheme
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Composable
fun AddStory(
    viewModel: AddStoryViewModel = hiltViewModel(),
    savedStateHandle: SavedStateHandle,
    onSubmit: () -> Unit,
    onRecordStory: () -> Unit
) {
    viewModel.updateSaveState(savedStateHandle)
    val uiState by viewModel.addStoryUIState.collectAsState(AddStoryUIState())
    AddStory(
        addStoryUiState = uiState,
        onAudioPickerClick = { viewModel.onAudioUri(it) },
        onImagePickerClick = { viewModel.onImageUri(it) },
        onPhotoResult = { viewModel.onPhotoResult() },
        onSubmit = {
            viewModel.addStory()
            onSubmit()
        },
        onImageFileCreated = { viewModel.setImageFile(it) },
        onTitleChange = { viewModel.setTitle(it) },
        onVoicedByChange = { viewModel.setVoicedBy(it) },
        onTypeChange = { viewModel.setType(StoryType.values()[it]) },
        onRecordStory = onRecordStory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStory(
    addStoryUiState: AddStoryUIState,
    onAudioPickerClick: (Uri) -> Unit,
    onImagePickerClick: (Uri) -> Unit,
    onTitleChange: (String) -> Unit,
    onVoicedByChange: (String) -> Unit,
    onTypeChange: (Int) -> Unit,
    onPhotoResult: () -> Unit,
    onImageFileCreated: (File) -> Unit,
    onSubmit: () -> Unit,
    onRecordStory: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Add a new story") })
        }
    ) { paddingValues ->
        val context = LocalContext.current
        val audioPickerLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                result.data?.data?.let { uri -> onAudioPickerClick(uri) }
            }
        val imagePickerLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                result.data?.data?.let { uri -> onImagePickerClick(uri) }
            }
        val takePictureLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                onPhotoResult()
            }
        val cameraPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    takePicture(takePictureLauncher, context) { onImageFileCreated(it) }
                }
            }
        var bottomSheetType by remember { mutableStateOf(AddStoryBottomSheetState.CLOSED) }
        val bottomSheetState = rememberModalBottomSheetState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), horizontalAlignment = Alignment.End
        ) {
            LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.weight(1f)) {
                item {
                    AddStoryTextField(
                        prompt = "Add a title",
                        hint = "eg. The Three Little Pigs",
                        value = addStoryUiState.title,
                        onValueChange = onTitleChange
                    )
                    Spacer(Modifier.height(24.dp))
                }
                item {
                    AddStoryTextField(
                        prompt = "Who made this?",
                        hint = "eg. Auntie Em",
                        value = addStoryUiState.voicedBy,
                        onValueChange = onVoicedByChange
                    )
                    Spacer(Modifier.height(24.dp))
                }
                item {
                    AddStoryRadioButtons(
                        options = StoryType.values().map { type -> type.name },
                        selectedIndex = addStoryUiState.type.value,
                        onSelected = { onTypeChange(it) })
                }
                item {
                    AddFile(
                        addStoryUiState.audioFilename,
                        stringResource(R.string.add_audio_file), R.drawable.note
                    ) {
                        bottomSheetType = AddStoryBottomSheetState.OPEN_AUDIO
                    }
                }
                item {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
                item {
                    AddFile(
                        selectedFileName = addStoryUiState.imageFilename,
                        hint = stringResource(R.string.add_thumbnail_image),
                        R.drawable.add_image
                    ) {
                        bottomSheetType = AddStoryBottomSheetState.OPEN_IMAGE
                    }
                }
            }
            Divider(modifier = Modifier.padding(bottom = 4.dp))
            IconButton(
                onClick = onSubmit,
                enabled = addStoryUiState.isValid(),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Submit")
            }
        }
        val scope = rememberCoroutineScope()
        fun dismissBottomSheet() = scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
            if (!bottomSheetState.isVisible) {
                bottomSheetType = AddStoryBottomSheetState.CLOSED
            }
        }

        val recordPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    onRecordStory()
                } else {
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.recording_audio_not_permitted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        if (bottomSheetType != AddStoryBottomSheetState.CLOSED) {
            ModalBottomSheet(onDismissRequest = {
                bottomSheetType = AddStoryBottomSheetState.CLOSED
            }, sheetState = bottomSheetState) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .fillMaxWidth()
                ) {
                    when (bottomSheetType) {
                        AddStoryBottomSheetState.OPEN_AUDIO -> {
                            AddMediaButton(
                                iconPainterResource = R.drawable.browse,
                                buttonTextResource = R.string.browse_audio_description
                            ) {
                                audioPickerLauncher.launch(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_GET_CONTENT).apply {
                                            type = "audio/*"
                                        }, "Pick the audio file for your story"
                                    )
                                )
                                dismissBottomSheet()
                            }
                            AddMediaButton(
                                iconPainterResource = R.drawable.record,
                                buttonTextResource = R.string.studio_toolbar_title
                            ) {
                                dismissBottomSheet()
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) != PERMISSION_GRANTED
                                ) {
                                    recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    onRecordStory()
                                }
                            }
                        }

                        else -> {
                            AddMediaButton(
                                iconPainterResource = R.drawable.browse,
                                buttonTextResource = R.string.browse_image_description
                            ) {
                                imagePickerLauncher.launch(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "image/*"
                                }, "Pick the image thumbnail for your story"))
                                dismissBottomSheet()
                            }
                            AddMediaButton(
                                iconPainterResource = R.drawable.camera,
                                buttonTextResource = R.string.take_a_picture
                            ) {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PERMISSION_GRANTED
                                ) {
                                    takePicture(takePictureLauncher, context) {
                                        onImageFileCreated(
                                            it
                                        )
                                    }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                                dismissBottomSheet()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMediaButton(
    @DrawableRes iconPainterResource: Int,
    @StringRes buttonTextResource: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(16.dp)
            .clickable { onClick() }
    ) {
        Icon(
            painter = painterResource(id = iconPainterResource),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .padding(8.dp)
                .size(56.dp)
        )
        Text(
            stringResource(id = buttonTextResource),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddAudioButtonPreview() {
    PillowBargeTheme {
        AddMediaButton(
            iconPainterResource = R.drawable.record,
            buttonTextResource = R.string.studio_toolbar_title
        ) {}
    }
}

@Composable
fun AddFile(
    selectedFileName: String?,
    hint: String,
    @DrawableRes iconPainter: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .height(56.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = selectedFileName ?: hint,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selectedFileName == null) FontWeight.Light else FontWeight.Bold,
            color = if (selectedFileName == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )
        Icon(
            painterResource(id = iconPainter),
            contentDescription = "Browse",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .padding(start = 16.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .size(48.dp)
                .padding(8.dp)
        )
    }
}

@Preview(showBackground = true, name = "Light mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, name = "Dark Mode"
)
@Composable
fun AddStory_Preview() {
    PillowBargeTheme {
        AddStory(AddStoryUIState(), {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Composable
@ExperimentalMaterial3Api
fun AddStoryTextField(
    prompt: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(8.dp)) {
        Text(
            prompt,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "light", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddStoryTextField_Preview() {
    PillowBargeTheme {
        AddStoryTextField(
            prompt = "Add a title",
            hint = "i.e. The Three Little Pigs",
            value = "",
            onValueChange = {})
    }
}

@Composable
fun AddStoryRadioButtons(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEachIndexed { index, option ->
            Column(horizontalAlignment = CenterHorizontally) {
                RadioButton(selected = index == selectedIndex, onClick = { onSelected(index) })
                Text(option, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true)
@Composable
fun AddStoryRadioButtons_Preview() {
    PillowBargeTheme {
        AddStoryRadioButtons(options = listOf("Story", "Song"), selectedIndex = 1, onSelected = {})
    }
}

fun takePicture(
    takePictureLauncher: ActivityResultLauncher<Intent>,
    context: Context,
    onFileCreated: (File) -> Unit
) {
    val file = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "${UUID.randomUUID()}.jpg"
    ).also {
        onFileCreated(it)
    }
    takePictureLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(
            MediaStore.EXTRA_OUTPUT,
            FileProvider.getUriForFile(
                context,
                "${context.applicationContext.packageName}.provider",
                file
            )
        )
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    })
}

enum class AddStoryBottomSheetState {
    CLOSED,
    OPEN_AUDIO,
    OPEN_IMAGE
}
