package de.yochyo.yummybooru.api.downloads

import android.util.SparseArray
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.events.events.DownloadManagerPageEvent
import de.yochyo.yummybooru.utils.liveData.LiveArrayList
import de.yochyo.yummybooru.utils.toTagArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

abstract class Manager(val tags: Array<String>) {
    companion object {
        private val lock = Any()
        private val map = HashMap<String, Manager>()
        fun get(tags: String): Manager {
            synchronized(lock) {
                val m = map[tags]
                if (m != null) return m
                else throw NullPointerException("Manager ($tags) was not yet initialized")
            }
        }

        fun getOrInit(tags: String): Manager {
            synchronized(lock) {
                val m = map[tags]
                if (m != null) return m
                else return initialize(tags)
            }
        }

        fun initialize(tags: String): Manager {
            synchronized(lock) {
                val m = object : Manager(tags.toTagArray()) {}
                map[tags] = m
                return m
            }
        }

        suspend fun reset(tags: String) {
            val m: Manager?
            synchronized(lock) { m = map[tags] }
            m?.reset()
        }

        suspend fun resetAll() {
            for (m in map.keys)
                reset(m)
        }
    }

    val posts = LiveArrayList<de.yochyo.yummybooru.api.Post>()
    private val pageStatus = SparseArray<PageStatus>()
    private val pages = SparseArray<List<de.yochyo.yummybooru.api.Post>>()
    var position = -1
    var currentPage = 0
        private set(value) {
            field = value
        }
    val currentPost: de.yochyo.yummybooru.api.Post?
        get() = posts[position]


    fun loadPage(page: Int): List<de.yochyo.yummybooru.api.Post>? {
        if (pageStatus[page] == PageStatus.DOWNLOADED) {
            val p = pages[page]
            if (p != null) {
                if (page > currentPage) {
                    currentPage = page
                    posts += p
                    println(page)
                }
                return p
            }
        }
        return null
    }

    suspend fun downloadPage(page: Int): List<de.yochyo.yummybooru.api.Post> {
        val status = pageStatus[page]
        var p = pages[page]
        if (status == null) {
            pageStatus.append(page, PageStatus.DOWNLOADING)
            p = Api.getPosts(page, tags)
            if (!posts.isEmpty) {
                val lastFromLastPage = posts.value!!.last()
                val samePost = p.find { it.id == lastFromLastPage.id }
                if (samePost != null)
                    p.takeWhile { it.id != samePost.id }
            }
            pages.append(page, p)
            pageStatus.append(page, PageStatus.DOWNLOADED)
            DownloadManagerPageEvent.trigger(DownloadManagerPageEvent(this, page, p))
        } else if (status == PageStatus.DOWNLOADING) {
            p = awaitPage(page)
        }
        return p
    }

    private suspend fun awaitPage(page: Int): List<de.yochyo.yummybooru.api.Post> {
        var list: List<de.yochyo.yummybooru.api.Post>? = null
        DownloadManagerPageEvent.registerSingleUseListener {
            if (it.manager == this && it.page == page) {
                list = it.posts
                return@registerSingleUseListener true
            }
            false
        }
        return withContext(Dispatchers.IO) {
            while (list == null)
                delay(50)
            list!!
        }
    }

    suspend fun reset() {
        pageStatus.clear()
        pages.clear()
        position = -1
        currentPage = 0
        withContext(Dispatchers.Main) { posts.clear() }
    }
}

private enum class PageStatus {
    DOWNLOADING, DOWNLOADED, INITIALIZED
}