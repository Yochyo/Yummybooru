package de.yochyo.ybooru.api.api

import de.yochyo.ybooru.database.entities.Server

class DanbooruApi(url: String) : Api(url) {


    override val name = "danbooru"
    override fun urlGetTag(name: String): String = "${url}tags.json?search[name_matches]=$name"
    override fun urlGetTags(beginSequence: String): String {
        return "${url}tags.json?search[name_matches]=$beginSequence*&limit=${Api.searchTagLimit}"
    }

    override fun urlGetPosts(page: Int, tags: Array<String>, limit: Int): String {
        return "${url}posts.json?limit=$limit&page=$page&login=${Server.currentServer.userName}&password_hash=${Server.currentServer.passwordHash}"
    }

    override fun urlGetNewest(): String = "${url}posts.json?limit=1&page=1"
}