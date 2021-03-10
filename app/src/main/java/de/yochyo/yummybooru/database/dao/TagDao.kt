package de.yochyo.yummybooru.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import de.yochyo.yummybooru.api.entities.Tag

@Dao
interface TagDao {
    @Query("SELECT * FROM tags")
    fun selectAll(): LiveData<List<Tag>>

    @Query("SELECT * FROM tags")
    fun selectAllNoFlow(): List<Tag>

    @Query("SELECT * FROM tags WHERE server_id = :serverId")
    fun selectWhere(serverId: Int): LiveData<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tags: Collection<Tag>): List<Long>

    @Delete
    fun delete(tag: Tag): Int

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(tag: Tag): Int

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(tags: List<Tag>): Int
}