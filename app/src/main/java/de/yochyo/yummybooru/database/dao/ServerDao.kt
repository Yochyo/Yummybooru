package de.yochyo.yummybooru.database.dao

import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.converter.ConvertBoolean
import org.jetbrains.anko.db.*

class ServerDao(database: ManagedSQLiteOpenHelper) : Dao(database) {
    private companion object {
        const val TABLE_NAME = "servers"
        const val NAME = "name"
        const val API = "api"
        const val URL = "url"
        const val USERNAME = "userName"
        const val PASSWORD = "password"
        const val ENABLE_R18_FILTER = "enableR18Filter"
        const val ID = "id"

        val parser = object : RowParser<Server> {
            override fun parseRow(columns: Array<Any?>): Server {
                return Server(columns[0] as String, columns[1] as String, columns[2] as String, columns[3] as String,
                        columns[4] as String, ConvertBoolean.toBoolean((columns[5] as Long).toInt()), (columns[6] as Long).toInt())
            }
        }
    }

    override fun createTable() {
        database.use {
            createTable(TABLE_NAME, true,
                    NAME to TEXT + NOT_NULL,
                    API to TEXT + NOT_NULL,
                    URL to TEXT + NOT_NULL,
                    USERNAME to TEXT + NOT_NULL,
                    PASSWORD to TEXT + NOT_NULL,
                    ENABLE_R18_FILTER to INTEGER + NOT_NULL,
                    ID to INTEGER + NOT_NULL + PRIMARY_KEY)
        }
    }

    fun delete(server: Server) {
        database.use {
            delete(TABLE_NAME, "$NAME = {$NAME} AND $ID = {$ID}", NAME to server.name, ID to server.id)
        }
    }

    fun insert(server: Server) {
        database.use {
            insert(TABLE_NAME,
                    NAME to server.name,
                    API to server.api,
                    URL to server.api,
                    USERNAME to server.userName,
                    PASSWORD to server.password,
                    ENABLE_R18_FILTER to ConvertBoolean.toInteger(server.enableR18Filter),
                    ID to server.id)
        }
    }

    fun selectAll(): List<Server> {
        return database.use {
            select(TABLE_NAME).exec { parseList(parser) }
        }
    }

    fun select(id: Int): Server {
        return database.use {
            select(TABLE_NAME).whereArgs("$ID = {$ID}", ID to id).exec { parseSingle(parser) }
        }
    }

    fun update(server: Server) {
        database.use {
            update(TABLE_NAME,
                    NAME to server.name,
                    API to server.api,
                    URL to server.api,
                    USERNAME to server.userName,
                    PASSWORD to server.password,
                    ENABLE_R18_FILTER to ConvertBoolean.toInteger(server.enableR18Filter),
                    ID to server.id).whereArgs("$ID = {$ID}", ID to server.id).exec()
        }
    }
}