package de.yochyo.yummybooru.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import de.yochyo.yummybooru.api.entities.Tag

class TagCollectionWithTags(
    @Embedded var collection: TagCollection,
    @Relation(parentColumn = "collectionId", entityColumn = "tagId", associateBy = Junction(TagCollectionTagCrossRef::class))
    var tags: List<Tag>
) {
    override fun toString(): String = collection.name
}