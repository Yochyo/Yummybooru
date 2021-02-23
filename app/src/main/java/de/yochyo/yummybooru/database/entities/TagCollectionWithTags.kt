package de.yochyo.yummybooru.database.entities

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Junction
import androidx.room.Relation
import de.yochyo.eventcollection.EventCollection
import de.yochyo.yummybooru.api.entities.Tag

class TagCollectionWithTags(
    @Embedded var collection: TagCollection,
    @Relation(parentColumn = "collectionId", entityColumn = "tagId", associateBy = Junction(TagCollectionTagCrossRef::class))
    private val _tags: List<Tag>
) {
    constructor(name: String, serverId: Int) : this(TagCollection(name, serverId), emptyList())

    @Ignore
    val tags = EventCollection(_tags as MutableCollection<Tag>)

    override fun toString(): String = collection.name
}