package de.yochyo.yummybooru.database.dao

import androidx.room.*
import de.yochyo.yummybooru.database.entities.TagCollection
import de.yochyo.yummybooru.database.entities.TagCollectionTagCrossRef
import de.yochyo.yummybooru.database.entities.TagCollectionWithTags

@Dao
interface TagCollectionDao {
    @Query("SELECT * FROM TagCollection")
    fun selectAll(): List<TagCollectionWithTags>

    @Query("SELECT * FROM TagCollection WHERE serverId = :serverId")
    fun selectWhere(serverId: Int): TagCollectionWithTags

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCollection(collection: TagCollection): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCollections(collections: List<TagCollection>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCollectionCrossRef(ref: TagCollectionTagCrossRef)

    @Delete
    fun deleteCrossRef(ref: TagCollectionTagCrossRef)

    @Delete
    fun deleteCollection(collection: TagCollection)

    @Update
    fun updateCollection(collection: TagCollection)
}