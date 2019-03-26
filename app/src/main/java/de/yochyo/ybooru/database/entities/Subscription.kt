package de.yochyo.ybooru.database.entities

import android.arch.persistence.room.*
import de.yochyo.ybooru.R
import de.yochyo.ybooru.database.database
import java.util.*

@Entity(tableName = "subs")
class Subscription(@PrimaryKey val name: String, val type: Int, var last: Int, var current: Int, val isFavorite: Boolean = false, val creation: Date = Date()) : Comparable<Subscription> {
    val color: Int
        get() {
            when (type) {
                Tag.GENERAL -> return R.color.blue
                Tag.CHARACTER -> return R.color.green
                Tag.COPYPRIGHT -> return R.color.violet
                Tag.ARTIST -> return R.color.dark_red
                Tag.META -> return R.color.orange
                else -> return R.color.white
            }
        }

    override fun toString() = "id:>$current $name"

    override fun compareTo(other: Subscription): Int {
        if (database.sortSubsByFavorite) {
            if (isFavorite && !other.isFavorite)
                return -1
            if (!isFavorite && other.isFavorite)
                return 1
        }
        if (database.sortSubsByAlphabet) return name.compareTo(other.name)
        return creation.compareTo(other.creation)
    }
}

@Dao
interface SubscriptionDao {
    @Insert
    fun insert(sub: Subscription)

    @Query("SELECT * FROM subs")
    fun getAllSubscriptions(): List<Subscription>

    @Delete
    fun delete(sub: Subscription)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(sub: Subscription)
}