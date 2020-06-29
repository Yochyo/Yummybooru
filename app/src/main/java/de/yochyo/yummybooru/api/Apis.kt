package de.yochyo.yummybooru.api

import de.yochyo.booruapi.api.DanbooruApi
import de.yochyo.booruapi.api.GelbooruApi
import de.yochyo.booruapi.api.IBooruApi
import de.yochyo.booruapi.api.MoebooruApi
import java.util.*

object Apis {
    val apis = arrayListOf("danbooru", "moebooru", "gelbooru")
    fun getApi(name: String, url: String): IBooruApi {
        return when (name.toLowerCase(Locale.ENGLISH)) {
            "moebooru" -> return MoebooruApi(url)
            "gelbooru" -> return GelbooruApi(url)
            else -> DanbooruApi(url)
        }
    }
}