package de.yochyo.booruapi.api.moebooru

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import de.yochyo.booruapi.api.BooruUtils
import de.yochyo.booruapi.api.IBooruApi
import de.yochyo.booruapi.utils.encodeUTF8
import de.yochyo.json.JSONArray
import de.yochyo.json.JSONObject
import java.security.MessageDigest

open class MoebooruApi(override val host: String) : IBooruApi, BooruUtils() {
    private val mapper = JsonMapper.builder().apply {
        addModule(KotlinModule())
        propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }.build()

    private var username = ""
    private var password = ""

    override suspend fun login(username: String, password: String): Boolean {
        this.username = username
        this.password = passwordToHash(password)
        return true
    }

    override suspend fun getTagAutoCompletion(begin: String, limit: Int): List<MoebooruTag>? {
        val json = getJsonArrayFromUrl("${host}tag.json?name=${encodeUTF8(begin)}*&limit=$limit&search[order]=count")
        return json?.mapNotNull { if (it is JSONObject) parseTagFromJson(it) else null }
    }

    override suspend fun getPosts(page: Int, tags: String, limit: Int): List<MoebooruPost>? {
        val url = "${host}post.json?limit=$limit&page=$page&login=$username&password_hash=$password&tags=${encodeUTF8(tags)}"
        val json = getJsonArrayFromUrl(url)
        return json?.mapNotNull { if (it is JSONObject) parsePostFromJson(it) else null }
    }

    override suspend fun getTag(name: String): MoebooruTag {
        val json = if (name == "*") JSONArray() else getJsonArrayFromUrl("${host}tag.json?name=${encodeUTF8(name)}*")
        return when {
            json == null || json.isEmpty -> getDefaultTag(name)
            else -> {
                val tag = parseTagFromJson(json.getJSONObject(0))
                if (tag?.name == name) tag
                else getDefaultTag(name)
            }
        }
    }

    private suspend fun getDefaultTag(name: String): MoebooruTag {
        val newestID = getNewestPost()?.id ?: 0
        return MoebooruTag(-1, name, MoebooruTag.MOEBOORU_UNKNOWN, newestID, false)
    }

    protected fun passwordToHash(password: String): String {
        val byteArray = "choujin-steiner--$password--".toByteArray(charset = Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(byteArray)
        val digestBytes = digest.digest()
        val digestStr = StringBuilder()
        for (b in digestBytes)
            digestStr.append(String.format("%02x", b))
        return digestStr.toString()
    }


    fun parsePostFromJson(json: JSONObject): MoebooruPost? = try {
        mapper.readValue(json.toString(), MoebooruPost::class.java).apply {
            moebooruApi = this@MoebooruApi
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    fun parseTagFromJson(json: JSONObject): MoebooruTag? = try {
        mapper.readValue(json.toString(), MoebooruTag::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

}