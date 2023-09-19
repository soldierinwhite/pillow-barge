package io.soldierinwhite.pillowbarge.studio

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.soldierinwhite.pillowbarge.R
import io.soldierinwhite.pillowbarge.ui.theme.PillowBargeTheme
import io.soldierinwhite.pillowbarge.ui.theme.light_error
import io.soldierinwhite.pillowbarge.ui.theme.transparent

@Composable
fun Studio(
    viewModel: StudioViewModel = hiltViewModel(),
    onSubmit: (String?, Uri?) -> Unit
) {
    Studio(
        onRecord = { viewModel.record() },
        onStop = { viewModel.stop() },
        onSubmit = {
            viewModel.save().let { onSubmit(it.first, it.second) }
        },
        onDiscard = { viewModel.discard() },
        isRecording = viewModel.isRecording.collectAsStateWithLifecycle().value,
        canSave = viewModel.canSave.collectAsStateWithLifecycle().value
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Studio(
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onSubmit: () -> Unit,
    onDiscard: () -> Unit,
    isRecording: Boolean,
    canSave: Boolean,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.studio_toolbar_title)) })
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(Modifier.weight(2f))
            IconButton(
                onClick = { if (isRecording) onStop() else onRecord() },
                modifier = Modifier
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.run { if (isRecording) primary else transparent },
                        CircleShape
                    )
                    .size(108.dp)
            ) {
                Icon(
                    painterResource(if (isRecording) R.drawable.stop_circle else R.drawable.record),
                    contentDescription = stringResource(if (isRecording) R.string.stop_recording else R.string.start_recording),
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.run { if (isRecording) onPrimary else light_error }
                )
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = canSave) {
                Row {
                    IconButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(56.dp),
                        onClick = onDiscard,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.discard)
                        )
                    }
                    IconButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(56.dp),
                        onClick = onSubmit,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.submit)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light mode")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun Studio_NotRecording_Preview() {
    PillowBargeTheme {
        Studio(
            onRecord = { },
            onStop = {},
            onSubmit = {},
            onDiscard = {},
            canSave = true,
            isRecording = false
        )
    }
}

@Preview(showBackground = true, name = "Light mode")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun Studio_Recording_Preview() {
    PillowBargeTheme {
        Studio(
            onRecord = { },
            onStop = {},
            onSubmit = {},
            onDiscard = {},
            canSave = true,
            isRecording = true
        )
    }
}
