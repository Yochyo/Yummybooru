package de.yochyo.yummybooru.api.entities

import android.content.Context
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import de.yochyo.booruapi.api.IBooruApi
import de.yochyo.booruapi.api.pixiv.PixivApi2
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.observable.IObservableObject
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Apis
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.utils.general.toBooruTag
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

@Entity(tableName = "servers")
class Server(
    @NonNull name: String,
    @NonNull url: String,
    apiName: String,
    @NonNull username: String = "",
    @NonNull password: String = "",
    @PrimaryKey(autoGenerate = true) @NonNull var id: Int = 0
) : Comparable<Server>,
    IObservableObject<Server,
            Int> {

    val headers
        get() = api.getHeaders()

    @Ignore
    var api: IBooruApi = Apis.getApi(apiName, url)
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

    @NonNull
    @ColumnInfo(name = "api")
    var apiName = apiName
        set(value) {
            field = value
            GlobalScope.launch {
                api = Apis.getApi(value, url)
                api.login(username, password)
                trigger(CHANGED_API)
            }
        }

    @NonNull
    var username = username
        set(value) {
            field = value
            GlobalScope.launch { api.login(value, password) }
            trigger(CHANGED_USERNAME)
        }

    @NonNull
    var password = password
        set(value) {
            field = value
            GlobalScope.launch { api.login(username, value) }
            trigger(CHANGED_PASSWORD)
        }

    @Ignore
    override val onChange = EventHandler<OnChangeObjectEvent<Server, Int>>()

    @Ignore
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

    @Deprecated("temp method cause I have no time")
    private fun login(context: Context) {
        GlobalScope.launch {
            val api = this@Server.api
            if (api is PixivApi2) {
                val refreshToken = context.preferences.prefs.getString("pixiv-$username", null)
                if (refreshToken == null) {
                    if (api.login(username, password)) {
                        val ref = api.refreshToken
                        if (ref != null) {
                            context.preferences.setPreference("pixiv-$username", ref)
                            api.login("X", ref)
                        }
                    }
                } else {
                    if (!api.login("", refreshToken)) {
                        if (api.login(username, password)) {
                            val ref = api.refreshToken
                            if (ref != null) {
                                context.preferences.setPreference("pixiv-$username", ref)
                                api.login("", ref)
                            }
                        }
                    }
                }

            } else api.login(username, password)
        }
    }

    private fun trigger(change: Int) = onChange.trigger(OnChangeObjectEvent(this, change))

    suspend fun getMatchingTags(context: Context, beginSequence: String, limit: Int = 10) = api.getTagAutoCompletion(beginSequence, limit)?.map { it.toBooruTag(context) }
    suspend fun getTag(context: Context, name: String): Tag = api.getTag(name).toBooruTag(context)
    suspend fun getPosts(page: Int, tags: Array<String>, limit: Int = 30) = api.getPosts(page, tags.joinToString(" "), limit)
    suspend fun newestID() = api.getNewestPost()?.id


    fun isSelected(context: Context): Boolean = context.db.currentServer.id == id

    override fun compareTo(other: Server) = id.compareTo(other.id)
    override fun toString() = name
    override fun equals(other: Any?): Boolean {
        if (other is Server)
            return other.id == id
        return false
    }
}