package io.soldierinwhite.pillowbarge.model.story

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM story")
    fun getAll(): List<Story>

    @Query("SELECT * FROM story")
    fun getAllFlow(): Flow<List<Story>>

    @Insert
    fun insert(vararg stories: Story)

    @Delete
    fun delete(vararg stories: Story)

    @Update
    fun update(vararg stories: Story)
}
