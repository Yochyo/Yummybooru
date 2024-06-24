package de.yochyo.booruapi.api.gelbooru_beta

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import de.yochyo.booruapi.api.BooruUtils
import de.yochyo.booruapi.api.IBooruApi
import de.yochyo.booruapi.api.autocompletion.ITagAutoCompletion
import de.yochyo.booruapi.utils.encodeUTF8
import de.yochyo.json.JSONArray
import de.yochyo.json.JSONObject
import de.yochyo.json.XML
import java.text.SimpleDateFormat
import java.util.*

open class GelbooruBetaApi(
    override val host: String,
    private val autoCompletionMethod: ITagAutoCompletion<GelbooruBetaApi, GelbooruBetaTag> = GelbooruBetaAutoCompletionMethods.WITH_PHP_SCRIPT
) : IBooruApi, BooruUtils() {
    private val mapper = JsonMapper.builder().apply {
        addModule(KotlinModule())
        defaultDateFormat(SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.UK))
        propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }.build()

    private var username = ""
    private var password = ""

    override suspend fun login(username: String, password: String): Boolean {
        this.username = username
        this.password = password
        return true;
    }

    override suspend fun getTagAutoCompletion(begin: String, limit: Int): List<GelbooruBetaTag>? {
        return autoCompletionMethod.getTagAutoCompletion(this, begin, limit)
    }

    override suspend fun getTag(name: String): GelbooruBetaTag {
        val url = "$host/index.php?page=dapi&s=tag&q=index&json=1&api_key=$password&user_id=$username&limit=1&name=${encodeUTF8(name)}"

        val json =
            if (name == "*") JSONArray()
            else {
                val xml = getStringFromUrl(url) ?: return getDefaultTag(name)
                xml.let { XML.toJSONObject(it) }?.let { if (it.has("tags")) it.getJSONObject("tags") else null }
                    ?.let { if (it.has("tag")) it.get("tag") else JSONArray() }.let {
                        when (it) {
                            is JSONObject -> JSONArray().apply { put(it) }
                            is JSONArray -> it
                            else -> null
                        }
                    }
            }

        return when {
            json == null || json.isEmpty -> getDefaultTag(name)
            else -> {
                val tag = parseTagFromJson(json.getJSONObject(0))
                if (tag?.name == name) tag
                else getDefaultTag(name)
            }
        }
    }

    private suspend fun getDefaultTag(name: String): GelbooruBetaTag {
        val newestID = getNewestPost()?.id ?: 0
        return GelbooruBetaTag(-1, name, GelbooruBetaTag.GELBOORU_BETA_UNKNOWN, newestID, false)
    }

    override suspend fun getPosts(page: Int, tags: String, limit: Int): List<GelbooruBetaPost>? {
        val pid = (page - 1)
        val url = "$host/index.php?page=dapi&s=post&q=index&api_key=$password&user_id=$username&limit=$limit&pid=$pid&tags=${encodeUTF8(tags)}"
        val xml = getStringFromUrl(url) ?: return null
        val jsonParent = XML.toJSONObject(xml)
        val json = jsonParent.let { if (it.has("posts")) it.getJSONObject("posts") else null }?.let { if (it.has("post")) it.get("post") else JSONArray() }
        return when (json) {
            is JSONObject -> listOf(parsePostFromJson(json)).mapNotNull { it }
            is JSONArray -> json.mapNotNull { if (it is JSONObject) parsePostFromJson(it) else null }
            else -> null
        }
    }

    fun parsePostFromJson(json: JSONObject): GelbooruBetaPost? = try {
        println(json.toString())
        mapper.readValue(json.toString(), GelbooruBetaPost::class.java).apply {
            gelbooruBetaApi = this@GelbooruBetaApi
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    fun parseTagFromJson(json: JSONObject): GelbooruBetaTag? = try {
        mapper.readValue(json.toString(), GelbooruBetaTag::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}