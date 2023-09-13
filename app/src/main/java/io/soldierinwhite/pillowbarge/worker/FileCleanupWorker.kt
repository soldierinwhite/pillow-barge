package io.soldierinwhite.pillowbarge.worker

import android.content.Context
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.soldierinwhite.pillowbarge.model.story.StoryDao
import java.time.Duration

@HiltWorker
class FileCleanupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val storyDao: StoryDao
) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        val usedUris = storyDao.getAll().flatMap { listOf(it.imageUri, it.audioUri) }
        context.filesDir.listFiles()?.forEach {
            if (it.toUri().toString() !in usedUris) {
                it.delete()
            }
        }
        return Result.success()
    }

}

val fileCleanupWorkRequest = PeriodicWorkRequestBuilder<FileCleanupWorker>(Duration.ofDays(1))
    .build()
