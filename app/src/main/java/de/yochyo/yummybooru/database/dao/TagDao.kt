package de.yochyo.yummybooru.database.dao

import androidx.room.*
import de.yochyo.yummybooru.api.entities.Tag

@Dao
interface TagDao {
    @Query("SELECT * FROM tags")
    fun selectAll(): List<Tag>

    @Query("SELECT * FROM tags WHERE server_id = :serverId")
    fun selectWhere(serverId: Int): List<Tag>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tags: Collection<Tag>)

    @Delete
    fun delete(tag: Tag)

    @Update
    fun update(tag: Tag)
}