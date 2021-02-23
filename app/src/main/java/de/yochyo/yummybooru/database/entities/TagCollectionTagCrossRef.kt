package de.yochyo.yummybooru.database.entities

import androidx.room.Entity

@Entity(primaryKeys = ["collectionId", "tagId"])
class TagCollectionTagCrossRef(val collectionId: Int, val tagId: Int)