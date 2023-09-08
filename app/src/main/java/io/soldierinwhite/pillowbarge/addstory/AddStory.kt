package io.soldierinwhite.pillowbarge.addstory

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.soldierinwhite.pillowbarge.R
import io.soldierinwhite.pillowbarge.addstory.AddStoryViewModel.AddStoryUIState
import io.soldierinwhite.pillowbarge.ui.theme.PillowBargeTheme

@Composable
fun AddStory(
    viewModel: AddStoryViewModel = hiltViewModel(), onSubmit: () -> Unit
) {
    AddStory(
        addStoryUiState = viewModel.addStoryUIState.collectAsState().value,
        onAudioPickerClick = { viewModel.onAudioUri(it) },
        onImagePickerClick = { viewModel.onImageUri(it) },
        onSubmit = {
            viewModel.addStory()
            onSubmit()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStory(
    addStoryUiState: AddStoryUIState,
    onAudioPickerClick: (Uri) -> Unit,
    onImagePickerClick: (Uri) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Add a new story") })
        }
    ) {
        val audioPickerLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                result.data?.data?.let { onAudioPickerClick(it) }
            }
        val imagePickerLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                result.data?.data?.let { onImagePickerClick(it) }
            }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it), horizontalAlignment = Alignment.End
        ) {
            LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.weight(1f)) {
                item {
                    AddFile(
                        addStoryUiState.audioFilename, "Add audio file", R.drawable.note
                    ) {
                        audioPickerLauncher.launch(
                            Intent.createChooser(
                                Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "audio/*"
                                }, "Pick the audio file for your story"
                            )
                        )
                    }
                }
                item {
                    Divider()
                }
                item {
                    AddFile(
                        selectedFileName = addStoryUiState.imageFilename,
                        hint = "Add thumbnail image",
                        R.drawable.add_image
                    ) {
                        imagePickerLauncher.launch(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "image/*"
                        }, "Pick the image thumbnail for your story"))
                    }
                }
            }
            Divider(modifier = Modifier.padding(bottom = 4.dp))
            IconButton(
                onClick = onSubmit,
                enabled = addStoryUiState.run { audioFilename != null && imageFilename != null },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = "Submit")
            }
        }
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
        AddStory(AddStoryUIState(null, null), {}, {}, {})
    }
}
