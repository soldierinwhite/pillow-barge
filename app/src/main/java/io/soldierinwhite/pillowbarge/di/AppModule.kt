package io.soldierinwhite.pillowbarge.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.soldierinwhite.pillowbarge.room.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun database(application: Application): AppDatabase =
        Room.databaseBuilder(
            application,
            AppDatabase::class.java, "pillow-barge"
        ).build()

    @Provides
    @Singleton
    fun storyDao(appDatabase: AppDatabase) = appDatabase.storyDao()

    @Provides
    @Singleton
    fun applicationContext(application: Application): Context = application.applicationContext
}
