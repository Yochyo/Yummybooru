package de.yochyo.ybooru.api

import de.yochyo.ybooru.R
import org.json.JSONObject
import java.util.*

class Tag(val name: String, val type: Int = UNKNOWN, var isFavorite: Boolean = false, val creation: Date? = null) {
    companion object {
        const val GENERAL = 0
        const val CHARACTER = 4
        const val COPYPRIGHT = 3
        const val ARTIST = 1
        const val META = 5
        const val UNKNOWN = 99

        fun getTagFromJson(json: JSONObject): Tag? {
            return try {
                var type = json.getInt("category")
                if (type !in 0..5)
                    type = UNKNOWN
                Tag(json.getString("name"), type)
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
}

class Subscription(val name: String, val startID: Int, var currentID: Int)