package de.yochyo.yummybooru.api.entities

import android.content.Context
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.utils.general.passwordToHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

data class Server(var name: String, var api: String, var url: String, var userName: String = "", var password: String = "", var enableR18Filter: Boolean = false, val id: Int = -1) : Comparable<Server> {

    companion object {
        private var _currentServer: Server? = null
        fun getCurrentServer(context: Context): Server{
            if (_currentServer == null || _currentServer!!.id != context.db.currentServerID) {
                _currentServer = context.db.getServer(context.db.currentServerID) ?:
                        if(context.db.servers.isNotEmpty()) context.db.servers.first()
                        else Server("", "", "", "", "") //In case no server exist because of whatever bug may happened
            }
            return _currentServer!!
        }
        fun getCurrentServerID(context: Context): Int = getCurrentServer(context).id
    }

    private var cachedPassword = password

    var passwordHash: String = if(cachedPassword == "") "" else passwordToHash(password)
        get() {
            if (cachedPassword != password) {
                cachedPassword = password
                field = if(cachedPassword == "") "" else passwordToHash(password)
            }
            return field
        }
        private set

    val urlHost: String = try {
        if(url == "") ""
        else URL(url).host
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }

    fun isSelected(context: Context): Boolean = getCurrentServerID(context) == id

    suspend fun select(context: Context) {
        withContext(Dispatchers.Main) {
            if (getCurrentServer(context) != this@Server)
                SelectServerEvent.trigger(SelectServerEvent(context, getCurrentServer(context), this@Server))
            context.db.currentServerID = id
            Api.selectApi(api, url)
            context.db.loadServerWithMutex()
            context.db.servers.notifyChange()
            updateMissingTypeTags(context)
            updateMissingTypeSubs(context)
        }
    }

    override fun compareTo(other: Server) = id.compareTo(other.id)
    override fun toString() = name
    override fun equals(other: Any?): Boolean {
        if (other is Server)
            return other.id == id
        return false
    }

    fun deleteServer(context: Context) {
        GlobalScope.launch { context.db.deleteServer(id) }
    }

    private fun updateMissingTypeTags(context: Context) {
        GlobalScope.launch {
            val current = getCurrentServer(context)
            val oldTags = context.db.tags.toCollection(ArrayList())
            val newTags = ArrayList<Tag>()
            for (tag in oldTags) { //Tags updaten
                if (tag.type == Tag.UNKNOWN) {
                    val t = Api.getTag(context, tag.name)
                    newTags += t.copy(isFavorite = tag.isFavorite, creation = tag.creation, serverID = tag.serverID)
                }
            }
            for (tag in newTags) { //Tags ersetzen
                if (getCurrentServer(context) == current) {
                    if (tag.type != Tag.UNKNOWN) {
                        context.db.deleteTag(tag.name)
                        context.db.addTag(tag)
                    }
                } else break
            }
        }
    }

    private fun updateMissingTypeSubs(context: Context) {
        GlobalScope.launch {
            val current = getCurrentServer(context)
            val oldSubs = context.db.subs.toCollection(ArrayList())
            val newSubs = ArrayList<Subscription>()
            for (sub in oldSubs) { //Tags updaten
                if (sub.type == Tag.UNKNOWN) {
                    val s = Subscription.fromTag(context, Api.getTag(context, sub.name))
                    newSubs += s.copy(isFavorite = sub.isFavorite, creation = sub.creation, serverID = sub.serverID)
                }
            }
            for (sub in newSubs) { //Tags ersetzen
                if (current == getCurrentServer(context)) {
                    if (sub.type != Tag.UNKNOWN) {
                        context.db.deleteSubscription(sub.name)
                        context.db.addSubscription(sub)
                    }
                } else break
            }
        }
    }
}