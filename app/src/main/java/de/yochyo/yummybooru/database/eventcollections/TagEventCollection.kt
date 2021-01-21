package de.yochyo.yummybooru.database.eventcollections

import android.content.Context
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.SortedTreeWithPrimaryKey

class TagEventCollection(comparator: Comparator<Tag>) : SortedTreeWithPrimaryKey<Tag, String>(comparator, { it.name }) {
    companion object {
        fun getInstance(context: Context) = TagEventCollection(context.db.getTagComparator())
    }
}