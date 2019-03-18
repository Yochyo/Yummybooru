package de.yochyo.ybooru.api

import android.content.Context
import de.yochyo.ybooru.R
import de.yochyo.ybooru.database
import org.json.JSONObject
import java.util.*

open class Tag(val context: Context, val name: String, val type: Int = UNKNOWN, var isFavorite: Boolean = false, val creation: Date? = null) : Comparable<Tag> {
    companion object {
        const val GENERAL = 0
        const val CHARACTER = 4
        const val COPYPRIGHT = 3
        const val ARTIST = 1
        const val META = 5
        const val UNKNOWN = 99

        fun getTagFromJson(context: Context, json: JSONObject): Tag? {
            return try {
                var type = json.getInt("category")
                if (type !in 0..5)
                    type = UNKNOWN
                Tag(context, json.getString("name"), type)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    val color: Int
        get() {
            when (type) {
                GENERAL -> return R.color.blue
                CHARACTER -> return R.color.green
                COPYPRIGHT -> return R.color.violet
                ARTIST -> return R.color.dark_red
                META -> return R.color.orange
                else -> return R.color.white
            }
        }

    override fun toString(): String {
        return name
    }

    override fun compareTo(other: Tag): Int {
        if (context.database.sortTagsByFavorite) {
            if (isFavorite && !other.isFavorite)
                return -1
            if (!isFavorite && other.isFavorite)
                return 1
        }
        if (context.database.sortTagsByAlphabet) return name.compareTo(other.name)
        if (creation != null && other.creation != null) return creation.compareTo(other.creation)
        return 0
    }
}