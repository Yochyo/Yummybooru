package de.yochyo.yummybooru.api

import de.yochyo.booruapi.api.*
import java.util.*

object Apis {
    val DANBOORU = "Danbooru"
    val MOEBOORU = "Moebooru"
    val GELBOORU = "Gelbooru"
    val MY_IMOUTO = "MyImouto"
    val apis = arrayListOf(DANBOORU, MOEBOORU, GELBOORU, MY_IMOUTO)
    fun getApi(name: String, url: String): IBooruApi {
        return when (name.toLowerCase(Locale.ENGLISH)) {
            MOEBOORU -> return MoebooruApi(url)
            GELBOORU -> return GelbooruApi(url)
            MY_IMOUTO -> MyImoutoApi(url)
            else -> DanbooruApi(url)
        }
    }
}