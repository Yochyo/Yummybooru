package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

object TagBackup : BackupableEntity<Tag> {
    override fun toString(e: Tag, context: Context): String {
        return "${e.name};${e.type};${e.isFavorite};${e.creation.time};${e.serverID};${e.count}"
    }

    override fun toEntity(s: String, context: Context) {
        val split = s.split(";")
        val iter = split.iterator()
        GlobalScope.launch { db.tagDao.insert(Tag(iter.next(), iter.next().toInt(), iter.next().toBoolean(), Date(iter.next().toLong()), iter.next().toInt(), iter.next().toInt())) }
    }

}