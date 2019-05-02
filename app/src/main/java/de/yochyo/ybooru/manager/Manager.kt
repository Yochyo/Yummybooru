package de.yochyo.ybooru.manager

import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.api.api.Api
import de.yochyo.ybooru.database.liveData.LiveArrayList
import de.yochyo.ybooru.utils.toTagArray
import java.lang.Exception

abstract class Manager(val tags: Array<String>) {
    companion object {
        private val map = HashMap<String, Manager>()
        fun get(tags: String): Manager {
            val m = map[tags]
            if (m != null) return m
            else throw NullPointerException("Manager ($tags) was not yet initialized")
        }

        fun getOrInit(tags: String): Manager {
            val m = map[tags]
            if (m != null) return m
            else return initialize(tags)
        }

        fun initialize(tags: String): Manager {
            val manager = map[tags]
            if (manager == null) {
                val m = object : Manager(tags.toTagArray()) {}
                map[tags] = m
                return m
            } else return manager
        }

        fun reset(tags: String) {
            val m = map[tags]
            m?.reset()
        }

        fun resetAll() {
            for (m in map.keys)
                reset(m)
        }
    }

    val posts = LiveArrayList<Post>()
    private val pages = HashMap<Int, List<Post>>()
    var position = -1
    var currentPage = 0
        private set(value) {
            field = value
        }
    val currentPost: Post?
        get() = posts[position]


    fun loadPage(page: Int): List<Post>? {
        val p = pages[page]
        if (p != null) {
            if (page > currentPage) {
                currentPage = page
                posts += p
            }
            return p
        } else return null
    }

    suspend fun downloadPage(page: Int): List<Post> {
        var p = pages[page]
        if (p == null) {
            p = Api.getPosts(page, tags)
            //avoid doubled posts
            if (!posts.isEmpty) {
                val lastFromLastPage = posts.value!!.last()
                val samePost = p.find { it.id == lastFromLastPage.id }
                if (samePost != null)
                    p.takeWhile { it.id != samePost.id }
            }
            pages[page] = p
        }
        return p
    }

    fun reset() {
        pages.clear()
        position = -1
        currentPage = 0
        posts.clear()
        println("|||||||||||||${posts.size}")
    }
}