package de.yochyo.ybooru.utils.backup

import android.content.Context
import de.yochyo.ybooru.database.db

object PreferencesBackup : BackupableEntity<String>{
    override fun toString(e: String, context: Context): String {
        return "${db.nextServerID};${db.limit};${db.currentServerID};${db.sortTags};${db.sortSubs};${db.downloadOriginal};${db.getSavePath(context)}"
    }
    override fun toEntity(s: String, context: Context) {
        val split = s.split(";")
        val iter = split.iterator()
        db.nextServerID = iter.next().toInt()
        db.limit = iter.next().toInt()
        db.currentServerID = iter.next().toInt()
        db.sortTags = iter.next()
        db.sortSubs = iter.next()
        db.downloadOriginal = iter.next().toBoolean()
        db.setSavePath(iter.next())
    }
}