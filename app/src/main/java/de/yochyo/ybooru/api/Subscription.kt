package de.yochyo.ybooru.api

import android.content.Context
import de.yochyo.ybooru.database
import java.util.*

class Subscription(context: Context, name: String, type: Int, var lastID: Int, var currentID: Int, creation: Date? = null) : Tag(context, name, type, false, creation), Comparable<Tag> {
    val tagString: String
        get() = "id:>$currentID $name"

    override fun compareTo(other: Tag): Int {
        if (context.database.sortSubsByFavorite) {
            if (isFavorite && !other.isFavorite)
                return -1
            if (!isFavorite && other.isFavorite)
                return 1
        }
        if (context.database.sortSubsByAlphabet) return name.compareTo(other.name)
        if (creation != null && other.creation != null) return creation.compareTo(other.creation)
        return 0
    }
}