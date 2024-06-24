package de.yochyo.booruapi.api.gelbooru_beta

import de.yochyo.booruapi.api.BooruUtils
import de.yochyo.booruapi.api.autocompletion.ITagAutoCompletion
import de.yochyo.booruapi.utils.encodeUTF8
import de.yochyo.json.JSONArray
import de.yochyo.json.JSONObject
import de.yochyo.json.XML

object GelbooruBetaAutoCompletionMethods {
    private val utils = BooruUtils()

    val WITH_TAG_COLLECTION = object : ITagAutoCompletion<GelbooruBetaApi, GelbooruBetaTag> {
        override suspend fun getTagAutoCompletion(api: GelbooruBetaApi, begin: String, limit: Int): List<GelbooruBetaTag>? {
            val url = "${api.host}/index.php?page=dapi&s=tag&q=index&json=1&limit=$limit&name_pattern=${encodeUTF8(begin)}"
            val xml = utils.getStringFromUrl(url) ?: return null
            val jsonParent = XML.toJSONObject(xml)
            val json = jsonParent.getJSONObject("tags").let { if (it.has("tag")) it.get("tag") else JSONArray() }
            return when {
                json is JSONObject -> listOf(api.parseTagFromJson(json)).mapNotNull { it }
                json is JSONArray -> json.mapNotNull { if (it is JSONObject) api.parseTagFromJson(it) else null }
                else -> null
            }
        }
    }
    val WITH_PHP_SCRIPT = object : ITagAutoCompletion<GelbooruBetaApi, GelbooruBetaTag> {
        override suspend fun getTagAutoCompletion(api: GelbooruBetaApi, begin: String, limit: Int): List<GelbooruBetaTag>? {
            val url = "${api.host}/autocomplete.php?q=$begin"
            val json = utils.getJsonArrayFromUrl(url) ?: return null
            return json.mapNotNull { if (it is JSONObject) getTag(it) else null }
        }

        private fun getTag(json: JSONObject): GelbooruBetaTag? {
            return try {
                val name = json.getString("value")
                val label = json.getString("label")
                val count = label.substringAfter(" ").filter { it != '(' && it != ')' }.toInt()
                GelbooruBetaTag(-1, name, GelbooruBetaTag.GELBOORU_BETA_UNKNOWN, count, false)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}