package de.yochyo.yummybooru.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import de.yochyo.yummybooru.database.entities.TagCollection
import de.yochyo.yummybooru.database.entities.TagCollectionTagCrossRef
import de.yochyo.yummybooru.database.entities.TagCollectionWithTags

@Dao
interface TagCollectionDao {
    @Query("SELECT * FROM TagCollection")
    fun selectAll(): LiveData<List<TagCollectionWithTags>>

    @Query("SELECT * FROM TagCollection WHERE serverId = :serverId")
    fun selectWhere(serverId: Int): LiveData<List<TagCollectionWithTags>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCollection(collection: TagCollection): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCollections(collections: List<TagCollection>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCollectionCrossRef(ref: TagCollectionTagCrossRef): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCollectionCrossRef(ref: List<TagCollectionTagCrossRef>): List<Long>

    @Delete
    fun deleteCrossRef(ref: TagCollectionTagCrossRef): Int

    @Delete
    fun deleteCollection(collection: TagCollection): Int

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun updateCollection(collection: TagCollection): Int
}