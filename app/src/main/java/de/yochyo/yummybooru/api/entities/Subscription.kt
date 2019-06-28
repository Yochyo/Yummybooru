package de.yochyo.yummybooru.api.entities

import android.arch.persistence.room.*
import de.yochyo.yummybooru.api.api.Api
import java.util.*
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db

@Entity(tableName = "subs", primaryKeys = ["name", "serverID"])
data class Subscription(val name: String, val type: Int, val lastID: Int, val lastCount: Int, val isFavorite: Boolean = false, val creation: Date = Date(), val serverID: Int = Server.currentID) : Comparable<Subscription> {
    companion object{

        suspend fun fromTag(tag: Tag): Subscription{
            val t = Api.getTag(tag.name)
            return if(t != null)
                Subscription(t.name, t.type, Api.newestID(), t.count, false, Date(), t.serverID)
            else Subscription(tag.name, tag.type, Api.newestID(), tag.count, false, Date(), tag.serverID)
        }
    }

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

    override fun toString() = "id:>$lastID $name"

    override fun compareTo(other: Subscription): Int {
        if (db.sortSubsByFavorite) {
            if (isFavorite && !other.isFavorite)
                return -1
            if (!isFavorite && other.isFavorite)
                return 1
        }
        if (db.sortSubsByAlphabet) return name.compareTo(other.name)
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

    @Update
    fun update(sub: Subscription)
}
