package de.yochyo.booruapi.api.gelbooru_beta

import de.yochyo.booruapi.BooruTagSelector
import org.jsoup.nodes.Element

//TODO comments
object GelbooruBetaUtils {
    private val selector = object : BooruTagSelector<GelbooruBetaTag, Int>() {

        override fun getType(element: Element): Int {
            val type = element.className()
            if (type.contains("tag-type-general")) return GelbooruBetaTag.GELBOORU_BETA_GENERAL
            if (type.contains("tag-type-artist")) return GelbooruBetaTag.GELBOORU_BETA_ARTIST
            if (type.contains("tag-type-copyright")) return GelbooruBetaTag.GELBOORU_BETA_COPYRIGHT
            if (type.contains("tag-type-character")) return GelbooruBetaTag.GELBOORU_BETA_CHARACTER
            if (type.contains("tag-type-metadata")) return GelbooruBetaTag.GELBOORU_BETA_META
            return GelbooruBetaTag.GELBOORU_BETA_UNKNOWN
        }

        override fun getName(element: Element): String {
            val elements = element.select("a").filter { it -> it.text() != "?" }
            return elements.firstOrNull()?.text() ?: "null"
        }

        override fun toTag(name: String, type: Int): GelbooruBetaTag {
            return GelbooruBetaTag(0, name, type, 0, false)
        }

    }

    suspend fun parseTagsFromUrl(host: String, id: Int): List<GelbooruBetaTag>? {
        return selector.parse("$host/index.php?page=post&s=view&id=$id")?.map { it.copy(name = it.name.replace(" ", "_")) }
    }
}
