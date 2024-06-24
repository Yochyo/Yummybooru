package de.yochyo.booruapi

import de.yochyo.booruapi.api.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

abstract class BooruTagSelector<E : Tag, T> {
    protected abstract fun toTag(name: String, type: T): E

    protected abstract fun getName(element: Element): String
    protected abstract fun getType(element: Element): T
    suspend fun parse(url: String): List<E>? {
        return withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0").get()
                val list = doc.select("#tag-sidebar").firstOrNull()
                val elements = list?.select("li[class*=tag-type]")
                elements?.map { toTag(getName(it), getType(it)) }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}