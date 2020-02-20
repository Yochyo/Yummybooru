package de.yochyo.yummybooru.api.entities

import android.content.Context
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import java.util.*

data class Subscription(val context: Context, val name: String, val type: Int, val lastID: Int, val lastCount: Int, val isFavorite: Boolean = false, val creation: Date = Date(), val serverID: Int = Server.getCurrentServerID(context)) : Comparable<Subscription> {
    companion object {
        suspend fun fromTag(context: Context, tag: Tag): Subscription {
            val t = Api.getTag(context, tag.name)
            return Subscription(context, t.name, t.type, Api.newestID(context), t.count, false, Date(), t.serverID)
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
        if (isFavorite && !other.isFavorite)
            return -1
        if (!isFavorite && other.isFavorite)
            return 1
        return name.compareTo(other.name)
    }
}