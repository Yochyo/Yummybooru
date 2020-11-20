package de.yochyo.yummybooru.api

import de.yochyo.booruapi.api.IBooruApi
import de.yochyo.booruapi.api.danbooru.DanbooruApi
import de.yochyo.booruapi.api.gelbooru.GelbooruApi
import de.yochyo.booruapi.api.moebooru.MoebooruApi
import de.yochyo.booruapi.api.my_imouto.MyImoutoApi
import java.util.*

object Apis {
    val DANBOORU = "Danbooru"
    val MOEBOORU = "Moebooru"
    val GELBOORU = "Gelbooru"
    val MY_IMOUTO = "MyImouto"
    val apis = arrayListOf(DANBOORU, MOEBOORU, GELBOORU, MY_IMOUTO)
    fun getApi(name: String, url: String): IBooruApi {
        return when (name.toLowerCase(Locale.ENGLISH)) {
            MOEBOORU.toLowerCase(Locale.ENGLISH) -> MoebooruApi(url)
            GELBOORU.toLowerCase(Locale.ENGLISH) -> GelbooruApi(url)
            MY_IMOUTO.toLowerCase(Locale.ENGLISH) -> MyImoutoApi(url)
            else -> DanbooruApi(url)
        }
    }
}