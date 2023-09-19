package io.soldierinwhite.pillowbarge.worker

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.soldierinwhite.pillowbarge.model.story.StoryDao
import java.io.File

@HiltWorker
class FileCleanupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val storyDao: StoryDao
) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        val stories = storyDao.getAll()
        val usedUris = stories.flatMap { listOf(it.imageUri, it.audioUri) }
        context.filesDir.listFiles()?.forEach {
            if (it.toUri().toString() !in usedUris) {
                it.delete()
            }
        }
        stories.filter { story ->
            Uri.parse(story.audioUri).path?.let { !File(it).exists() } ?: false
        }.toTypedArray().takeIf { it.isNotEmpty() }?.let { missingStories ->
            storyDao.delete(*missingStories)
        }
        stories.filter { story ->
            story.imageUri?.let { Uri.parse(it).path?.let { path -> !File(path).exists() } }
                ?: false
        }.toTypedArray().takeIf { it.isNotEmpty() }?.let { missingImages ->
            storyDao.update(*missingImages.map {
                it.copy(
                    imageUri = null
                )
            }.toTypedArray())
        }
        return Result.success()
    }

}
