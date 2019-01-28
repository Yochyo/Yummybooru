package de.yochyo.ybooru.api

import de.yochyo.ybooru.R
import java.util.*

class Tag(val name: String, val type: String = UNKNOWN, var isFavorite: Boolean = false, val creation: Date? = null) {
    companion object {
        const val GENERAL = "g"
        const val CHARACTER = "c"
        const val COPYPRIGHT = "cr"
        const val ARTIST = "a"
        const val META = "m"
        const val UNKNOWN = "u"
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
        return "[$name (type = $type)(isFavorite = $isFavorite)]"
    }
}

class Subscription(val name: String, val startID: Int, var currentID: Int)