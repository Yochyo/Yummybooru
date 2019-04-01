package de.yochyo.ybooru.api.api

import de.yochyo.ybooru.database.database

class DanbooruApi(url: String) : Api(url) {


    override val name = "danbooru"
    override fun urlGetTag(name: String): String = "https://danbooru.donmai.us/tags.json?search[name_matches]=$name"
    override fun urlGetTags(beginSequence: String): String {
        return "https://danbooru.donmai.us/tags.json?search[name_matches]=$beginSequence*&limit=${Api.searchTagLimit}"
    }

    override fun urlGetPosts(page: Int, tags: Array<String>, limit: Int): String {
        println(database.currentServerID)
        println(database.currentServer)
        return "https://danbooru.donmai.us/posts.json?limit=$limit&page=$page&login=${database.currentServer!!.userName}&password_hash=${database.currentServer!!.passwordHash}"
    }

    override fun urlGetNewest(): String = "https://danbooru.donmai.us/posts.json?limit=1&page=1"
}