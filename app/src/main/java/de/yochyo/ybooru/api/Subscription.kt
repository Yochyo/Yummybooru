package de.yochyo.ybooru.api

import android.content.Context
import de.yochyo.ybooru.database

class Subscription(val context: Context, val tag: Tag, var lastID: Int, var currentID: Int) : Comparable<Subscription> {
    val tagString: String
        get() = "id:>$currentID ${tag.name}"

    override fun compareTo(other: Subscription): Int {
        if (context.database.sortSubsByFavorite) {
            if (tag.isFavorite && !other.tag.isFavorite)
                return -1
            if (!tag.isFavorite && other.tag.isFavorite)
                return 1
        }
        if (context.database.sortSubsByAlphabet) return tag.name.compareTo(other.tag.name)
        if (tag.creation != null && other.tag.creation != null) return tag.creation.compareTo(other.tag.creation)
        return 0
    }
}