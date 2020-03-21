package de.yochyo.yummybooru.api

import de.yochyo.booruapi.api.DanbooruApi
import de.yochyo.booruapi.api.IApi
import de.yochyo.booruapi.api.MoebooruApi
import java.util.*

object Apis {
    val apis = arrayListOf("moebooru", "danbooru")
    fun getApi(name: String, url: String): IApi {
        return when (name.toLowerCase(Locale.ENGLISH)) {
            "moebooru" -> return MoebooruApi(url)
            else -> DanbooruApi(url)
        }
    }
}