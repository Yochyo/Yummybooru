package de.yochyo.ybooru.manager

import android.content.Context
import de.yochyo.ybooru.api.Api
import de.yochyo.ybooru.api.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                val m = object : Manager(tags.split(" ").toTypedArray()) {}
                map[tags] = m
                return m
            } else
                return manager
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

    val dataSet = ArrayList<Post>(200)
    private val pages = HashMap<Int, List<Post>>()
    var position = -1
    var _currentPage = 0
    val currentPage: Int
        get() = _currentPage
    val currentPost: Post?
        get() = dataSet[position]


    suspend fun getAndInitPage(context: Context, page: Int): List<Post> {
        val p = getOrDownloadPage(context, page)
        if (page > currentPage) {
            _currentPage = page
            withContext(Dispatchers.Main) { dataSet += p }
        }
        return p
    }

    suspend fun getOrDownloadPage(context: Context, page: Int): List<Post> {
        var p = pages[page]
        if (p == null) {
            val posts = Api.getPosts(context, page, *tags)
            p = posts
            if (dataSet.isNotEmpty()) {
                val lastFromLastPage = dataSet.last()
                val samePost = p.find { it.id == lastFromLastPage.id }
                if (samePost != null)
                    p.takeWhile { it.id != samePost.id }
            }
            pages[page] = p
        }
        return p
    }

    fun reset() {
        dataSet.clear()
        position = -1
        _currentPage = 0
        pages.clear()
    }
}