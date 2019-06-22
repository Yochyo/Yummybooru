package de.yochyo.ybooru.api.downloads

import android.util.SparseArray
import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.api.api.Api
import de.yochyo.ybooru.events.events.DownloadManagerPageEvent
import de.yochyo.ybooru.utils.liveData.LiveArrayList
import de.yochyo.ybooru.utils.toTagArray
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

    val posts = LiveArrayList<Post>()
    private val pageStatus = SparseArray<PageStatus>()
    private val pages = SparseArray<List<Post>>()
    var position = -1
    var currentPage = 0
        private set(value) {
            field = value
        }
    val currentPost: Post?
        get() = posts[position]


    fun loadPage(page: Int): List<Post>? {
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

    suspend fun downloadPage(page: Int): List<Post> {
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

    private suspend fun awaitPage(page: Int): List<Post> {
        var list: List<Post>? = null
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