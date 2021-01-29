package de.yochyo.yummybooru.database.dao

import android.database.sqlite.SQLiteDatabase
import de.yochyo.booruapi.api.TagType
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.converter.ConvertBoolean
import de.yochyo.yummybooru.database.converter.ConvertDate
import org.jetbrains.anko.db.*

class TagDao(database: ManagedSQLiteOpenHelper) : Dao(database) {
    val parser = rowParser { name: String, type: Long, serverID: Long, isFavorite: Long, creation: Long, lastCount: Long?, lastID: Long? ->
        val following = if (lastID == null || lastCount == null) null else Following(lastID.toInt(), lastCount.toInt())
        Tag(name, TagType.valueOf(type.toInt()), isFavorite > 0, 0, following, ConvertDate.toDate(creation), serverID.toInt())
    }

    private companion object {
        const val TABLE_NAME = "tags"
        const val NAME = "name"
        const val TYPE = "type"
        const val IS_FAVORITE = "isFavorite"
        const val CREATION = "creation"
        const val SERVER_ID = "server_id"
        const val LAST_COUNT = "last_count"
        const val LAST_ID = "last_id"

    }

    override fun createTable(database: SQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS tags(name TEXT NOT NULL, type INTEGER NOT NULL, server_id INTEGER NOT NULL, " +
                    "isFavorite INTEGER NOT NULL, creation INTEGER NOT NULL, last_count INTEGER, last_id INTEGER, " +
                    "PRIMARY KEY(name, server_id), " +
                    "FOREIGN KEY(server_id) REFERENCES servers(id) " +
                    "ON UPDATE CASCADE ON DELETE CASCADE)"
        )
    }

    fun delete(tag: Tag) {
        database.use {
            delete(TABLE_NAME, "$NAME = {$NAME} AND $SERVER_ID = {$SERVER_ID}", NAME to tag.name, SERVER_ID to tag.serverID)
        }
    }

    fun insert(tag: Tag) = insert(listOf(tag))

    fun insert(tags: Collection<Tag>) {
        database.use {
            transaction {
                for (tag in tags) {
                    insert(
                        TABLE_NAME,
                        NAME to tag.name,
                        TYPE to tag.type.value,
                        SERVER_ID to tag.serverID,
                        IS_FAVORITE to ConvertBoolean.toInteger(tag.isFavorite),
                        CREATION to ConvertDate.toTimestamp(tag.creation),
                        LAST_COUNT to tag.following?.lastCount,
                        LAST_ID to tag.following?.lastID
                    )
                }
            }
        }
    }

    fun selectWhere(server: Server): List<Tag> {
        return database.use {
            select(TABLE_NAME).whereArgs("$SERVER_ID = {$SERVER_ID}", SERVER_ID to server.id).exec { parseList(parser) }
        }
    }

    fun selectAll(): List<Tag> {
        return database.use {
            select(TABLE_NAME).exec { parseList(parser) }
        }
    }

    fun update(tag: Tag) {
        database.use {
            update(
                TABLE_NAME,
                TYPE to tag.type.value,
                IS_FAVORITE to ConvertBoolean.toInteger(tag.isFavorite),
                CREATION to ConvertDate.toTimestamp(tag.creation),
                LAST_COUNT to tag.following?.lastCount,
                LAST_ID to tag.following?.lastID
            ).whereArgs("$NAME = {$NAME} AND $SERVER_ID = {$SERVER_ID}", NAME to tag.name, SERVER_ID to tag.serverID).exec()
        }
    }
}