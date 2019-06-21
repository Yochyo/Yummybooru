package de.yochyo.ybooru.api.entities

import android.arch.persistence.room.*
import de.yochyo.ybooru.api.api.Api
import de.yochyo.ybooru.api.downloads.Manager
import de.yochyo.ybooru.database.db
import de.yochyo.ybooru.utils.lock
import de.yochyo.ybooru.utils.passwordToHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Entity(tableName = "servers")
data class Server(var name: String, var api: String, var url: String, var userName: String = "", var password: String = "", var enableR18Filter: Boolean = false, @PrimaryKey val id: Int = -1) : Comparable<Server> {
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
        get() = currentServer.id == id

    suspend fun select() {
            db.currentServerID = this.id
            _currentServer = this
            Api.initApi(this.api, this.url)
        withContext(Dispatchers.Main) {
            Manager.resetAll()
            synchronized(lock){
                db.initTags(id)
                db.initSubscriptions(id)
                db.servers.notifyChange()
            }
        }
    }

    fun updateMissingTypeTags() {
        GlobalScope.launch {
            val newTags = ArrayList<Tag>()
            for (tag in db.tags.value!!) { //Tags updaten
                if (tag.type == Tag.UNKNOWN && tag.name != "*") {
                    val t = Api.getTag(tag.name)
                    if (t != null) {
                        newTags += t.copy(isFavorite = tag.isFavorite, creation = tag.creation, serverID = tag.serverID)
                    }
                }
            }
            for (tag in newTags) { //Tags ersetzen
                db.deleteTag(tag.name)
                db.addTag(tag)
            }
        }
    }

    fun updateMissingTypeSubs() {
        GlobalScope.launch {
            val newSubs = ArrayList<Subscription>()
            for (sub in db.subs.value!!) { //Tags updaten
                if (sub.type == Tag.UNKNOWN && sub.name != "*") {
                    val t = Api.getTag(sub.name)
                    if (t != null) {
                        val s = Subscription.fromTag(t)
                        newSubs += s.copy(isFavorite = sub.isFavorite, creation = sub.creation, serverID = sub.serverID)
                    }
                }
            }
            for (sub in newSubs) { //Tags ersetzen
                db.deleteSubscription(sub.name)
                db.addSubscription(sub)
            }
        }
    }

    fun unselect() {
        synchronized(lock){
            db.currentServerID = -1
            _currentServer = null
            Api.instance = null
            db.tags.clear()
            db.subs.clear()
        }
    }

    fun deleteServer() {
        GlobalScope.launch { db.deleteServer(id) }
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