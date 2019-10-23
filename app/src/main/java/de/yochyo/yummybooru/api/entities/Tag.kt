package de.yochyo.yummybooru.api.entities

import androidx.room.*
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import java.util.*

@Entity(tableName = "tags", primaryKeys = ["name", "serverID"])
data class Tag(val name: String,val type: Int, val isFavorite: Boolean = false, val creation: Date = Date(), val serverID: Int = Server.currentID, val count: Int = 0) : Comparable<Tag> {
    companion object {
        const val GENERAL = 0
        const val CHARACTER = 4
        const val COPYPRIGHT = 3
        const val ARTIST = 1
        const val META = 5
        const val UNKNOWN = 99
        const val SPECIAL = 100

        fun isSpecialTag(name: String): Boolean {
            return name == "*" || name.startsWith("height") || name.startsWith("width") || name.startsWith("order") || name.startsWith("rating") || name.contains(" ")
        }

        fun getCorrectTagType(tagName: String, type: Int): Int {
            return if (type in 0..1 || type in 3..5) type
            else if (isSpecialTag(tagName)) SPECIAL
            else UNKNOWN
        }
    }

    val color: Int
        get() {
            return when (type) {
                GENERAL -> R.color.blue
                CHARACTER -> R.color.green
                COPYPRIGHT -> R.color.violet
                ARTIST -> R.color.dark_red
                META -> R.color.orange
                else -> R.color.white
            }
        }

    override fun toString(): String = name

    override fun compareTo(other: Tag): Int {
        if (db.sortTagsByFavorite) {
            if (isFavorite && !other.isFavorite)
                return -1
            if (!isFavorite && other.isFavorite)
                return 1
        }
        if (db.sortTagsByAlphabet) return name.compareTo(other.name)
        return creation.compareTo(other.creation)
    }
}

@Dao
interface TagDao {
    @Insert
    fun insert(tag: Tag)

    @Query("SELECT * FROM tags")
    fun getAllTags(): List<Tag>

    @Delete
    fun delete(tag: Tag)

    @Update
    fun update(tag: Tag)
}
