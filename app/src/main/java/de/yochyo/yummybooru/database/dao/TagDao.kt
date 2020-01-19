package de.yochyo.yummybooru.database.dao

import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.converter.ConvertBoolean
import de.yochyo.yummybooru.database.converter.ConvertDate
import org.jetbrains.anko.db.*

class TagDao(database: ManagedSQLiteOpenHelper) : Dao(database) {
    private companion object {
        const val TABLE_NAME = "tags"
        const val NAME = "name"
        const val TYPE = "type"
        const val IS_FAVORITE = "isFavorite"
        const val CREATION = "creation"
        const val SERVER_ID = "serverID"
        const val COUNT = "count"

        val parser = object : RowParser<Tag> {
            override fun parseRow(columns: Array<Any?>): Tag {
                return Tag(columns[0] as String, (columns[1] as Long).toInt(), ConvertBoolean.toBoolean((columns[2] as Long).toInt()),
                        ConvertDate.toDate(columns[3] as Long), (columns[4] as Long).toInt(), (columns[5] as Long).toInt())
            }
        }
    }

    override fun createTable() {
        database.use {
            createTable(TABLE_NAME, true,
                    NAME to TEXT + NOT_NULL + PRIMARY_KEY,
                    TYPE to INTEGER + NOT_NULL,
                    IS_FAVORITE to INTEGER + NOT_NULL,
                    CREATION to INTEGER + NOT_NULL,
                    SERVER_ID to INTEGER + NOT_NULL + PRIMARY_KEY,
                    COUNT to INTEGER + NOT_NULL)
        }
    }

    fun delete(tag: Tag) {
        database.use {
            delete(TABLE_NAME, "$NAME = {$NAME} AND $SERVER_ID = {$SERVER_ID}", NAME to tag.name, SERVER_ID to tag.serverID)
        }
    }

    fun insert(tag: Tag) {
        database.use {
            insert(TABLE_NAME,
                    NAME to tag.name,
                    TYPE to tag.type,
                    IS_FAVORITE to ConvertBoolean.toInteger(tag.isFavorite),
                    CREATION to tag.creation,
                    SERVER_ID to tag.serverID,
                    COUNT to tag.count)
        }
    }

    fun selectWhereID(serverID: Int): List<Tag> {
        return database.use {
            select(TABLE_NAME).whereArgs("$SERVER_ID = {$SERVER_ID}", SERVER_ID to serverID).exec { parseList(parser) }
        }
    }
    fun selectAll(): List<Tag> {
        return database.use {
            select(TABLE_NAME).exec { parseList(parser) }
        }
    }

    fun update(tag: Tag) {
        database.use {
            update(TABLE_NAME,
                    TYPE to tag.type,
                    IS_FAVORITE to ConvertBoolean.toInteger(tag.isFavorite),
                    CREATION to tag.creation,
                    COUNT to tag.count).whereArgs("$NAME = {$NAME} AND $SERVER_ID = {$SERVER_ID}", NAME to tag.name, SERVER_ID to tag.serverID).exec()
        }
    }
}