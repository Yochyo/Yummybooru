package de.yochyo.booruapi.api.danbooru

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import danbooru.DanbooruPost
import de.yochyo.booruapi.api.BooruUtils
import de.yochyo.booruapi.api.IBooruApi
import de.yochyo.booruapi.utils.encodeUTF8
import de.yochyo.json.JSONArray
import de.yochyo.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class DanbooruApi(override val host: String) : IBooruApi, BooruUtils() {
    private val mapper = JsonMapper.builder().apply {
        addModule(KotlinModule())
        defaultDateFormat(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.UK))
        propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }.build()

    protected var username = ""
    protected var apiKey = ""

    override suspend fun getTag(name: String): DanbooruTag {
        val json =
            if (name == "*") JSONArray()
            else getJsonArrayFromUrl("${host}tags.json?search[name_matches]=${encodeUTF8(name)}")

        return when {
            json == null || json.isEmpty -> getDefaultTag(name)
            else -> {
                val tag = parseTagFromJson(json.getJSONObject(0))
                if (tag?.name == name) tag
                else getDefaultTag(name)
            }
        }
    }

    private suspend fun getDefaultTag(name: String): DanbooruTag {
        val newestCount = getNewestPost()?.id ?: 0
        return DanbooruTag(-1, name, DanbooruTag.DANBOORU_UNKNOWN, newestCount, Date(), Date(), false)

    }

    override suspend fun getTagAutoCompletion(begin: String, limit: Int): List<DanbooruTag>? {
        val url = "${host}tags.json?search[name_matches]=${encodeUTF8(begin)}*&limit=$limit&search[order]=count"
        val json = getJsonArrayFromUrl(url)
        return json?.mapNotNull { if (it is JSONObject) parseTagFromJson(it) else null }
    }

    override suspend fun getPosts(page: Int, tags: String, limit: Int): List<DanbooruPost>? {
        var url = "${host}posts.json?limit=$limit&page=$page&tags=${encodeUTF8(tags)}"
        if (username != "" && apiKey != "") url += "&login=$username&api_key=$apiKey"
        val json = getJsonArrayFromUrl(url)
        return json?.mapNotNull { if (it is JSONObject) parsePostFromJson(it) else null }
    }

    fun parsePostFromJson(json: JSONObject): DanbooruPost? = try {
        mapper.readValue(json.toString(), DanbooruPost::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    fun parseTagFromJson(json: JSONObject): DanbooruTag? = try {
        mapper.readValue(json.toString(), DanbooruTag::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    override suspend fun login(username: String, password: String): Boolean {
        this.username = username
        this.apiKey = password
        return true
    }

    override fun getHeaders(): Map<String, String> {
        return mapOf(Pair("User-Agent", "Yummybooru user $username"))
    }


}
