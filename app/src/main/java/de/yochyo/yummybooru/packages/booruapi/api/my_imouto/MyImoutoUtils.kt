package de.yochyo.booruapi.api.my_imouto

import de.yochyo.booruapi.BooruTagSelector
import de.yochyo.booruapi.api.moebooru.MoebooruTag
import org.jsoup.nodes.Element
import java.util.Date

//TODO comments
object MyImoutoUtils {
    private val selector = object : BooruTagSelector<MyImoutoTag, Int>() {

        override fun getType(element: Element): Int {
            val type = element.attr("data-type")
            return when (type) {
                "general" -> MoebooruTag.MOEBOORU_GENERAL
                "copyright" -> MoebooruTag.MOEBOORU_COPYRIGHT
                "artist" -> MoebooruTag.MOEBOORU_ARTIST
                "circle" -> MoebooruTag.MOEBOORU_CIRCLE
                "style" -> MoebooruTag.MOEBOORU_META
                "character" -> MoebooruTag.MOEBOORU_CHARACTER
                else -> MoebooruTag.MOEBOORU_UNKNOWN
            }
        }

        override fun getName(element: Element): String {
            return element.attr("data-name")
        }

        override fun toTag(name: String, type: Int): MyImoutoTag {
            return MyImoutoTag(0, name, type, 0, "", Date(), false)
        }

    }

    suspend fun parseTagsFromUrl(host: String, id: Int): List<MyImoutoTag>? {
        return selector.parse("$host/post/show/$id")?.map { it.copy(name = it.name.replace(" ", "_")) }
    }
}