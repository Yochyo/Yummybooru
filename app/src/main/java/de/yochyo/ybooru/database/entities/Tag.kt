package de.yochyo.ybooru.database.entities

import android.arch.persistence.room.*
import de.yochyo.ybooru.R
import de.yochyo.ybooru.database.database
import java.util.*

@Entity(tableName = "tags", primaryKeys = ["name", "serverID"])
class Tag(val name: String, val type: Int, var isFavorite: Boolean = false, val creation: Date = Date(), val serverID: Int = Server.currentID) : Comparable<Tag> {

    companion object {
        const val GENERAL = 0
        const val CHARACTER = 4
        const val COPYPRIGHT = 3
        const val ARTIST = 1
        const val META = 5
        const val UNKNOWN = 99
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
        if (database.sortTagsByFavorite) {
            if (isFavorite && !other.isFavorite)
                return -1
            if (!isFavorite && other.isFavorite)
                return 1
        }
        if (database.sortTagsByAlphabet) return name.compareTo(other.name)
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
