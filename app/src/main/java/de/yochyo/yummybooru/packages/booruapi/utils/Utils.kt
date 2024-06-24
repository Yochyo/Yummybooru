package de.yochyo.booruapi.utils

import de.yochyo.booruapi.api.Post
import java.net.URLEncoder
import java.util.logging.Logger

val logger = Logger.getLogger("de.yochyo.BooruApi")

fun encodeUTF8(urlStr: String): String {
    return URLEncoder.encode(urlStr, "UTF-8")
}

fun String.extension(): String = this.substringAfterLast(".")

fun removeDuplicatesUpdateCachedList(cache: MutableCollection<Int>, posts: List<Post>): List<Post> {
    return posts.filter {
        if (!cache.contains(it.id)) {
            cache += it.id
            true
        } else false
    }
}