package io.soldierinwhite.pillowbarge.model.story

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.soldierinwhite.pillowbarge.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoryCard(
    story: Story,
    onStoryClick: (Story) -> Unit,
    onLongClick: (Story) -> Unit,
    modifier: Modifier = Modifier
) {
    Image(
        painter = Uri.parse(story.imageUri).path?.let {
            try {
                BitmapPainter(BitmapFactory.decodeFile(it).asImageBitmap())
            } catch (e: Exception) {
                null
            }
        } ?: painterResource(id = R.drawable.book),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = { onStoryClick(story) },
                onLongClick = { onLongClick(story) })
    )
}

@Entity
data class Story(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "image_uri") val imageUri: String?,
    @ColumnInfo(name = "audio_uri") val audioUri: String
)
