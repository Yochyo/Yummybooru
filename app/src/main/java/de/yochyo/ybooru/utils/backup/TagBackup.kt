package de.yochyo.ybooru.utils.backup

import android.content.Context
import de.yochyo.ybooru.database.db
import de.yochyo.ybooru.database.entities.Tag
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

object TagBackup: BackupableEntity<Tag>{
    override fun toString(e: Tag, context: Context): String {
        return "${e.name};${e.type};${e.isFavorite};${e.creation.time};${e.serverID};${e.count}"
    }

    override fun toEntity(s: String, context: Context) {
        val split = s.split(";")
        val iter = split.iterator()
        GlobalScope.launch { db.addTag(Tag(iter.next(), iter.next().toInt(), iter.next().toBoolean(), Date(iter.next().toLong()), iter.next().toInt(), iter.next().toInt())) }
    }

}