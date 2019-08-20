package de.yochyo.yummybooru.api.entities

import android.arch.persistence.room.*
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import java.lang.Exception
import java.util.*

@Entity(tableName = "tags", primaryKeys = ["name", "serverID"])
data class Tag(val name: String, val type: Int, val isFavorite: Boolean = false, val creation: Date = Date(), val serverID: Int = Server.currentID, val count: Int = 0) : Comparable<Tag> {

    companion object {
        fun isSpecialTag(name: String): Boolean{
            return name == "*" || name.startsWith("height") || name.startsWith("width") || name.startsWith("order") || name.startsWith("rating")
        }

        const val GENERAL = 0
        const val CHARACTER = 4
        const val COPYPRIGHT = 3
        const val ARTIST = 1
        const val META = 5
        const val UNKNOWN = 99
        const val SPECIAL = 100

        fun getCorrectTagType(tagName: String, id: Int): Int{
            return if(id in 0..1 || id in 3..5) id
            else if(isSpecialTag(tagName)) SPECIAL
            else UNKNOWN
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
