package de.yochyo.ybooru.utils.backup

import android.content.Context
import de.yochyo.ybooru.database.db
import de.yochyo.ybooru.database.entities.Server
import de.yochyo.ybooru.utils.parseURL
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
        GlobalScope.launch { db.addServer(server, server.id) }
    }

}