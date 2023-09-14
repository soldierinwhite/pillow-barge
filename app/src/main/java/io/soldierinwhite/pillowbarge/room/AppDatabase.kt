package io.soldierinwhite.pillowbarge.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.soldierinwhite.pillowbarge.model.story.Converters
import io.soldierinwhite.pillowbarge.model.story.Story
import io.soldierinwhite.pillowbarge.model.story.StoryDao

@Database(
    entities = [Story::class],
    version = 2,
    autoMigrations = [AutoMigration(
        from = 1,
        to = 2
    )],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
}
