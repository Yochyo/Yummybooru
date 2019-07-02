package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.parseURL
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object ServerBackup : BackupableEntity<Server> {
    override fun toString(e: Server, context: Context): String {
        return "${e.name};${e.api};${e.urlHost};${e.userName};${e.password};${e.enableR18Filter};${e.id}"
    }

    override fun toEntity(s: String, context: Context) {
        val split = s.split(";")
        val iter = split.iterator()
        val server = Server(iter.next(), iter.next(), parseURL(iter.next()), iter.next(), iter.next(), iter.next().toBoolean(), iter.next().toInt())
        GlobalScope.launch { db.addServer(context, server, server.id) }
    }

}