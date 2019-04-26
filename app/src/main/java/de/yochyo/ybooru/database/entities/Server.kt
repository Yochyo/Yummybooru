package de.yochyo.ybooru.database.entities

import android.arch.persistence.room.*
import de.yochyo.ybooru.api.api.Api
import de.yochyo.ybooru.database.db
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.passwordToHash
import java.net.URL

@Entity(tableName = "servers")
class Server(var name: String, var api: String, var url: String, var userName: String = "", var password: String = "", var enableR18Filter: Boolean = false, @PrimaryKey(autoGenerate = true) val id: Int = -1) : Comparable<Server> {
    companion object {
        private var _currentServer: Server? = null
        val currentServer: Server
            get() {
                if (_currentServer == null || _currentServer!!.id != db.currentServerID)
                    _currentServer = db.getServer(db.currentServerID)
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
    @Ignore
    val urlHost = URL(url).host


    override fun compareTo(other: Server): Int {
        return id.compareTo(other.id)
    }

    val isSelected: Boolean
        get() = Server.currentServer.id == id

    fun select() {
        db.currentServerID = this.id
        Server._currentServer = this
        val api = Api.initApi(this.api, this.url)
        db.tags.clear()
        db.tags += db.getAllTags(this.id)
        db.subs.clear()
        db.subs += db.getAllSubscriptions(this.id)
        Manager.resetAll()
    }

    fun unselect() {
        db.currentServerID = -1
        Server._currentServer = null
        Api.instance = null
        db.tags.clear()
        db.subs.clear()
    }

    fun deleteServer() {
        db.deleteServer(id)
    }

    override fun toString() = name
    override fun equals(other: Any?): Boolean {
        if (other is Server)
            return other.id == id
        return false
    }
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