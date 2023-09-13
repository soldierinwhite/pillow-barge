package io.soldierinwhite.pillowbarge

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import io.soldierinwhite.pillowbarge.model.story.StoryDao
import io.soldierinwhite.pillowbarge.worker.fileCleanupWorkRequest
import javax.inject.Inject

@HiltAndroidApp
class PillowBargeApplication : Application() {
    @Inject
    lateinit var storyDao: StoryDao
    override fun onCreate() {
        super.onCreate()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "cleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                fileCleanupWorkRequest
            )
    }
}
