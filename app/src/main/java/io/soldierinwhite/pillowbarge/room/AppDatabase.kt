package io.soldierinwhite.pillowbarge.room

import androidx.room.Database
import androidx.room.RoomDatabase
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryDao

@Database(entities = [Story::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
}
