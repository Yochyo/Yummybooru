package de.yochyo.yummybooru.api.entities

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import de.yochyo.booruapi.api.IBooruApi
import de.yochyo.booruapi.api.pixiv.PixivApi2
import de.yochyo.yummybooru.api.Apis
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.utils.general.toBooruTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Entity(tableName = "servers")
data class Server(
    val name: String,
    val url: String,
    @ColumnInfo(name = "api")
    val apiName: String,
    val username: String = "",
    val password: String = "",
    @PrimaryKey(autoGenerate = true) val id: Int = 0
) : Comparable<Server> {
    @Ignore
    var api: IBooruApi = Apis.getApi(apiName, url)
        private set

    val headers
        get() = api.getHeaders()

    @Ignore
    val urlHost: String = try {
        if (url == "") ""
        else URL(url).host
    } catch (e: Exception) {
        e.printStackTrace()
        ""
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

    suspend fun getMatchingTags(context: Context, beginSequence: String, limit: Int = 10) =
        withContext(Dispatchers.IO) { api.getTagAutoCompletion(beginSequence, limit)?.map { it.toBooruTag(this@Server) } }

    suspend fun getTag(name: String): Tag = withContext(Dispatchers.IO) { api.getTag(name).toBooruTag(this@Server) }
    suspend fun getPosts(page: Int, tags: Array<String>, limit: Int = 30) = withContext(Dispatchers.IO) { api.getPosts(page, tags.joinToString(" "), limit) }
    suspend fun newestID() = withContext(Dispatchers.IO) { api.getNewestPost()?.id }


    fun isSelected(context: Context): Boolean = context.preferences.selectedServerId == id

    override fun compareTo(other: Server) = id.compareTo(other.id)
    override fun toString() = name
    override fun equals(other: Any?): Boolean {
        if (other is Server)
            return other.id == id
        return false
    }
}