package de.yochyo.yummybooru.api.entities

import android.content.Context
import de.yochyo.booruapi.api.IApi
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.observable.IObservableObject
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Apis
import de.yochyo.yummybooru.utils.general.currentServer
import de.yochyo.yummybooru.utils.general.toBooruTag
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

open class Server(name: String, url: String, apiName: String, username: String = "", password: String = "", var id: Int = -1) : Comparable<Server>, IObservableObject<Server, Int> {
    var api: IApi = Apis.getApi(apiName, url)
        private set
    var name = name
        set(value) {
            field = value
            trigger(CHANGED_NAME)
        }
    var url = url
        set(value) {
            field = value
            GlobalScope.launch {
                api = Apis.getApi(apiName, value)
                api.login(username, password)
                trigger(CHANGED_URL)
            }
        }
    var apiName = apiName
        set(value) {
            field = value
            GlobalScope.launch {
                api = Apis.getApi(value, url)
                api.login(username, password)
                trigger(CHANGED_API)
            }
        }
    var username = username
        set(value) {
            field = value
            GlobalScope.launch { api.login(value, password) }
            trigger(CHANGED_USERNAME)
        }
    var password = password
        set(value) {
            field = value
            GlobalScope.launch { api.login(username, value) }
            trigger(CHANGED_PASSWORD)
        }
    override val onChange = EventHandler<OnChangeObjectEvent<Server, Int>>()

    val urlHost: String = try {
        if (url == "") ""
        else URL(url).host
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }

    companion object {
        const val CHANGED_NAME = 0
        const val CHANGED_URL = 1
        const val CHANGED_API = 2
        const val CHANGED_USERNAME = 3
        const val CHANGED_PASSWORD = 4
    }

    init {
        GlobalScope.launch { login(username, password) }
    }

    protected fun trigger(change: Int) = onChange.trigger(OnChangeObjectEvent(this, change))

    suspend fun login(username: String, password: String) = api.login(username, password)
    suspend fun getMatchingTags(context: Context,beginSequence: String, limit: Int = api.DEFAULT_TAG_LIMIT) = api.getMatchingTags(beginSequence, limit)?.map { it.toBooruTag(context) }
    suspend fun getTag(context: Context, name: String): Tag? = api.getTag(name)?.toBooruTag(context)
    suspend fun getPosts(page: Int, tags: Array<String>, limit: Int = api.DEFAULT_POST_LIMIT) = api.getPosts(page, tags, limit)
    suspend fun newestID() = api.newestID()


    fun isSelected(context: Context): Boolean = context.currentServer.id == id

    override fun compareTo(other: Server) = id.compareTo(other.id)
    override fun toString() = name
    override fun equals(other: Any?): Boolean {
        if (other is Server)
            return other.id == id
        return false
    }
}