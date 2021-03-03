package de.yochyo.yummybooru.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import de.yochyo.yummybooru.api.entities.Tag

open class TagCollectionWithTags(
    @Embedded val collection: TagCollection,
    @Relation(parentColumn = "collectionId", entityColumn = "tagId", associateBy = Junction(TagCollectionTagCrossRef::class))
    val tags: List<Tag>
) {
    constructor(name: String, serverId: Int) : this(TagCollection(name, serverId), emptyList())

    override fun toString(): String = collection.name
}