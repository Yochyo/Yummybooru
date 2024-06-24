package de.yochyo.booruapi.api.moebooru

import de.yochyo.booruapi.BooruTagSelector
import de.yochyo.booruapi.api.gelbooru_beta.GelbooruBetaTag
import org.jsoup.nodes.Element

//TODO comments
object MoebooruUtils {
    private val selector = object : BooruTagSelector<MoebooruTag, Int>() {

        override fun getType(element: Element): Int {
            val type = element.className()
            if (type.contains("tag-type-general")) return MoebooruTag.MOEBOORU_GENERAL
            if (type.contains("tag-type-artist")) return MoebooruTag.MOEBOORU_ARTIST
            if (type.contains("tag-type-copyright")) return MoebooruTag.MOEBOORU_COPYRIGHT
            if (type.contains("tag-type-character")) return MoebooruTag.MOEBOORU_CHARACTER
            if (type.contains("tag-type-style")) return MoebooruTag.MOEBOORU_META
            if (type.contains("tag-type-circle")) return MoebooruTag.MOEBOORU_CIRCLE
            return GelbooruBetaTag.GELBOORU_BETA_UNKNOWN
        }

        override fun getName(element: Element): String {
            val elements = element.select("a").filter { it -> it.text() != "?" }
            return elements.firstOrNull()?.text() ?: "null"
        }

        override fun toTag(name: String, type: Int): MoebooruTag {
            return MoebooruTag(0, name, type, 0, false)
        }

    }

    suspend fun parseTagsFromUrl(host: String, id: Int): List<MoebooruTag>? {
        return selector.parse("$host/post/show/$id")?.map { it.copy(name = it.name.replace(" ", "_")) }
    }
}
