package de.yochyo.yummybooru.database.dao

import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.database.converter.ConvertBoolean
import de.yochyo.yummybooru.database.converter.ConvertDate
import org.jetbrains.anko.db.*

class SubDao(database: ManagedSQLiteOpenHelper) : Dao(database) {

    private companion object {
        const val TABLE_NAME = "subs"
        const val NAME = "name"
        const val TYPE = "type"
        const val LAST_ID = "lastID"
        const val LAST_COUNT = "lastCount"
        const val IS_FAVORITE = "isFavorite"
        const val CREATION = "creation"
        const val SERVER_ID = "serverID"


        val parser = object : RowParser<Subscription> {
            override fun parseRow(columns: Array<Any?>): Subscription {
                return Subscription(columns[0] as String, (columns[1] as Long).toInt(), (columns[2] as Long).toInt(), (columns[3] as Long).toInt(),
                        ConvertBoolean.toBoolean((columns[4] as Long).toInt()),
                        ConvertDate.toDate(columns[5] as Long), (columns[6] as Long).toInt())
            }
        }
    }

    override fun createTable() {
        database.use {
            createTable("subs", true,
                    NAME to TEXT + NOT_NULL + PRIMARY_KEY,
                    TYPE to INTEGER + NOT_NULL,
                    LAST_ID to INTEGER + NOT_NULL,
                    LAST_COUNT to INTEGER + NOT_NULL,
                    IS_FAVORITE to INTEGER + NOT_NULL,
                    CREATION to INTEGER + NOT_NULL,
                    SERVER_ID to INTEGER + NOT_NULL + PRIMARY_KEY
            )
        }
    }

    fun insert(sub: Subscription) {
        database.use {
            insert(TABLE_NAME,
                    NAME to sub.name,
                    TYPE to sub.type,
                    LAST_ID to sub.lastID,
                    LAST_COUNT to sub.lastCount,
                    IS_FAVORITE to ConvertBoolean.toInteger(sub.isFavorite),
                    CREATION to sub.creation,
                    SERVER_ID to sub.serverID)
        }
    }

    fun selectWhereID(serverID: Int): List<Subscription> {
        return database.use {
            select(TABLE_NAME).whereArgs("$SERVER_ID = {$SERVER_ID}", SERVER_ID to serverID).exec { parseList(parser) }
        }
    }

    fun selectAll(): List<Subscription> {
        return database.use {
            select(TABLE_NAME).exec { parseList(parser) }
        }
    }

    fun delete(sub: Subscription) {
        database.use {
            delete(TABLE_NAME, "$NAME = {$NAME} AND $SERVER_ID = {$SERVER_ID}", NAME to sub.name, SERVER_ID to sub.serverID)
        }
    }

    fun update(sub: Subscription) {
        database.use {
            update(TABLE_NAME,
                    TYPE to sub.type,
                    LAST_ID to sub.lastID,
                    LAST_COUNT to sub.lastCount,
                    IS_FAVORITE to ConvertBoolean.toInteger(sub.isFavorite),
                    /*.whereArgs("$NAME = {$NAME} AND $SERVER_ID = {$SERVER_ID}", NAME to sub.name, SERVER_ID to sub.serverID).exec()*/
                    CREATION to sub.creation).whereArgs("$NAME = {$NAME} AND $SERVER_ID = {$SERVER_ID}", NAME to sub.name, SERVER_ID to sub.serverID).exec()
        }
    }
}
