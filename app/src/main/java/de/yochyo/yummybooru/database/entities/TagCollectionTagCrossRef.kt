package de.yochyo.yummybooru.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import de.yochyo.yummybooru.api.entities.Tag

@Entity(
    primaryKeys = ["collectionId", "tagId"],
    foreignKeys = [
        ForeignKey(onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE, entity = TagCollection::class, parentColumns = ["collectionId"], childColumns = ["collectionId"]),
        ForeignKey(onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE, entity = Tag::class, parentColumns = ["tagId"], childColumns = ["tagId"])
    ]
)
class TagCollectionTagCrossRef(val collectionId: Int, val tagId: Int)