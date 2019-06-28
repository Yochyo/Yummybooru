package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.database.db
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

object SubscriptionBackup : BackupableEntity<Subscription> {
    override fun toString(e: Subscription, context: Context): String {
        return "${e.name};${e.type};${e.lastID};${e.lastCount};${e.isFavorite};${e.creation.time};${e.serverID}"
    }

    override fun toEntity(s: String, context: Context) {
        val split = s.split(";")
        val iter = split.iterator()
        GlobalScope.launch { db.subDao.insert(Subscription(iter.next(), iter.next().toInt(), iter.next().toInt(), iter.next().toInt(), iter.next().toBoolean(), Date(iter.next().toLong()), iter.next().toInt())) }
    }
}