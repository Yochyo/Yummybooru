package de.yochyo.yummybooru.database.dao

import android.database.sqlite.SQLiteDatabase
import de.yochyo.yummybooru.api.entities.Server
import org.jetbrains.anko.db.*

class ServerDao(database: ManagedSQLiteOpenHelper) : Dao(database) {
    private companion object {
        const val TABLE_NAME = "servers"
        const val NAME = "name"
        const val URL = "url"
        const val API = "api"
        const val USERNAME = "username"
        const val PASSWORD = "password"
        const val ID = "id"

        val parser = rowParser { name: String, url: String, api: String, username: String, password: String, id: Long ->
            Server(name, url, api, username, password, id = id.toInt())
        }
    }

    override fun createTable(database: SQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS servers(name TEXT NOT NULL, url TEXT NOT NULL, api TEXT NOT NULL, username TEXT NOT NULL, password TEXT NOT NULL, id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT)")
    }

    fun delete(server: Server) {
        database.use {
            delete(TABLE_NAME, "$NAME = {$NAME} AND $ID = {$ID}", NAME to server.name, ID to server.id)
        }
    }

    fun insert(server: Server): Int {
        return database.use {
            insert(TABLE_NAME,
                    NAME to server.name,
                    URL to server.url,
                    API to server.apiName,
                    USERNAME to server.username,
                    PASSWORD to server.password,
                    ID to null)
        }.toInt()
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
                    URL to server.url,
                    API to server.apiName,
                    USERNAME to server.username,
                    PASSWORD to server.password,
                    ID to server.id).whereArgs("$ID = {$ID}", ID to server.id).exec()
        }
    }
}