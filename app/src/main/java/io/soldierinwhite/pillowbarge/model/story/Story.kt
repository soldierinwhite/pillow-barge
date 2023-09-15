package io.soldierinwhite.pillowbarge.model.story

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import io.soldierinwhite.pillowbarge.R
import io.soldierinwhite.pillowbarge.ui.theme.PillowBargeTheme

@Composable
fun StoryCard(
    story: Story,
    onStoryClick: (Story) -> Unit,
    onDeleteClick: () -> Unit,
    onEnqueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .clip(RoundedCornerShape(16.dp))
        .clickable { onStoryClick(story) }) {
        var expanded by remember { mutableStateOf(false) }
        val arrowRotation =
            animateFloatAsState(label = "arrowRotation", targetValue = if (expanded) 180f else 0f)
        Box {
            Image(
                painter = story.imageUri?.let { Uri.parse(it) }?.path?.let {
                    try {
                        BitmapPainter(BitmapFactory.decodeFile(it).asImageBitmap())
                    } catch (e: Exception) {
                        null
                    }
                } ?: painterResource(id = R.drawable.book),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
            IconButton(
                onClick = { expanded = !expanded }, modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.onSecondaryContainer),
                        CircleShape
                    )
                    .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape)
                    .size(32.dp)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(id = if (expanded) R.string.collapse else R.string.expand),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(arrowRotation.value),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.fillMaxWidth()) {
                StoryAction(
                    text = R.string.delete,
                    onClick = {
                        onDeleteClick()
                        expanded = false
                    },
                    icon = R.drawable.delete
                )
                StoryAction(
                    text = R.string.add_to_queue,
                    onClick = {
                        onEnqueue()
                        expanded = false
                    },
                    icon = R.drawable.queue
                )
            }
        }
    }
}

@Composable
fun StoryAction(
    @StringRes text: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int
) {
    Row(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .requiredHeight(48.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon), contentDescription = null, modifier = Modifier
                .size(48.dp)
                .padding(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun StoryAction_Preview() {
    PillowBargeTheme {
        StoryAction(text = R.string.delete, onClick = { }, icon = R.drawable.delete)
    }

}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun StoryCard_Preview() {
    PillowBargeTheme {
        StoryCard(
            story = Story(0, "", "", StoryType.Story, "", ""),
            onDeleteClick = {},
            onEnqueue = {},
            onStoryClick = {})
    }
}

@Entity
data class Story(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "title", defaultValue = "") val title: String,
    @ColumnInfo(name = "voiced_by", defaultValue = "") val voicedBy: String,
    @ColumnInfo(name = "type", defaultValue = "0") val type: StoryType = StoryType.Story,
    @ColumnInfo(name = "image_uri") val imageUri: String?,
    @ColumnInfo(name = "audio_uri") val audioUri: String
)

class Converters {
    @TypeConverter
    fun typeToInt(type: StoryType) = type.value

    @TypeConverter
    fun intToType(value: Int) = StoryType.fromValue(value)
}

enum class StoryType(val value: Int) {
    Story(0),
    Song(1);

    companion object {
        fun fromValue(value: Int) = StoryType.values().first { it.value == value }
    }
}
