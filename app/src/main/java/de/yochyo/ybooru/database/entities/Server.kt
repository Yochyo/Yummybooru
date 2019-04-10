package de.yochyo.ybooru.database.entities

import android.arch.persistence.room.*
import de.yochyo.ybooru.api.api.Api
import de.yochyo.ybooru.database.database
import de.yochyo.ybooru.utils.passwordToHash

@Entity(tableName = "servers")
class Server(var name: String, var api: String, var url: String, var userName: String = "", var password: String = "", @PrimaryKey(autoGenerate = true) val id: Int = -1) : Comparable<Server> {
    companion object {
        private var _currentServer: Server? = null
        val currentServer: Server
            get() {
                if (_currentServer == null || _currentServer!!.id != database.currentServerID)
                    _currentServer = database.getServer(database.currentServerID)
                return _currentServer!!
            }
        val currentID: Int
            get() = currentServer.id
    }

    @Ignore
    private var cachedPassword = password
    @Ignore
    var _passwordHash: String? = null
    val passwordHash: String
        get() {
            if (_passwordHash == null || cachedPassword != password) {
                cachedPassword = password
                if (cachedPassword == "")
                    _passwordHash = ""
                else
                    _passwordHash = passwordToHash(password)
            }
            return _passwordHash!!
        }


    override fun compareTo(other: Server): Int {
        return id.compareTo(other.id)
    }

    val isSelected: Boolean
        get() = Server.currentServer.id == id

    fun select() {
        database.currentServerID = this.id
        Server._currentServer = this
        val api = Api.initApi(this.api, this.url)
        database.tags.clear()
        database.tags += database.getAllTags(this.id)
        database.subs.clear()
        database.subs += database.getAllSubscriptions(this.id)
    }

    fun unselect() {
        database.currentServerID = -1
        Server._currentServer = null
        Api.instance = null
        database.tags.clear()
        database.subs.clear()
    }

    fun deleteServer() {
        database.deleteServer(id)
    }

    override fun toString() = name
}

@Dao
interface ServerDao {
    @Insert
    fun insert(server: Server)

    @Delete
    fun delete(server: Server)

    @Update
    fun update(server: Server)

    @Query("SELECT * FROM servers")
    fun getAllServers(): List<Server>
}