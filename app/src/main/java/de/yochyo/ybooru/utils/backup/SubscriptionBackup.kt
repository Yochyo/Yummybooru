package de.yochyo.ybooru.utils.backup

import android.content.Context
import de.yochyo.ybooru.database.db
import de.yochyo.ybooru.database.entities.Subscription
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

object SubscriptionBackup : BackupableEntity<Subscription>{
    override fun toString(e: Subscription, context: Context): String {
        return "${e.name};${e.type};${e.lastID};${e.lastCount};${e.isFavorite};${e.creation.time};${e.serverID}"
    }

    override fun toEntity(s: String, context: Context) {
        val split = s.split(";")
        val iter = split.iterator()
        GlobalScope.launch { db.addSubscription(Subscription(iter.next(), iter.next().toInt(), iter.next().toInt(), iter.next().toInt(), iter.next().toBoolean(), Date(iter.next().toLong()), iter.next().toInt())) }
    }
}