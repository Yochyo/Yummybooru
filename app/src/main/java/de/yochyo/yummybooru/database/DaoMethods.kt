package de.yochyo.yummybooru.database

import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.entities.TagCollection
import de.yochyo.yummybooru.database.entities.TagCollectionTagCrossRef

interface DaoMethods {
    val db: RoomDb

    fun addServer(server: Server) = db.serverDao.insert(server)
    fun addServer(servers: List<Server>) = db.serverDao.insert(servers)
    fun removeServer(server: Server) = db.serverDao.delete(server)
    fun updateServer(server: Server) = db.serverDao.update(server)

    fun addTag(tag: Tag) = db.tagDao.insert(tag)
    fun addTags(tags: List<Tag>) = db.tagDao.insert(tags)
    fun removeTag(tag: Tag) = db.tagDao.delete(tag)
    fun updateTag(tag: Tag) = db.tagDao.update(tag)
    fun updateTags(tags: List<Tag>) = db.tagDao.update(tags)

    fun addTagCollection(tagCollection: TagCollection) = db.tagCollectionDao.insertCollection(tagCollection)
    fun addTagCollections(tagCollections: List<TagCollection>) = db.tagCollectionDao.insertCollections(tagCollections)
    fun removeTagCollection(tagCollection: TagCollection) = db.tagCollectionDao.deleteCollection(tagCollection)
    fun updateTagCollection(tagCollection: TagCollection) = db.tagCollectionDao.updateCollection(tagCollection)

    fun addTagToCollection(tagCollection: TagCollection, tag: Tag) = db.tagCollectionDao.insertCollectionCrossRef(TagCollectionTagCrossRef(tagCollection.id, tag.id))
    fun addTagsToCollection(tagCollection: TagCollection, tags: List<Tag>) = db.tagCollectionDao.insertCollectionCrossRef(tags.map {
        TagCollectionTagCrossRef(tagCollection.id, it.id)
    })

    fun removeTagFromCollection(tagCollection: TagCollection, tag: Tag) = db.tagCollectionDao.deleteCrossRef(TagCollectionTagCrossRef(tagCollection.id, tag.id))
    fun removeTagsFromCollection(tagCollection: TagCollection, tags: List<Tag>) =
        db.tagCollectionDao.deleteCrossRefs(tags.map { TagCollectionTagCrossRef(tagCollection.id, it.id) })
}