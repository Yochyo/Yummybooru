package de.yochyo.yummybooru.api.downloads

import android.content.Context
import android.util.SparseArray
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventCollection
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.utils.toTagArray
import de.yochyo.yummybooru.utils.toTagString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class Manager(val tags: Array<String>) {
    companion object {
        var current: Manager = Manager("*".toTagArray())
    }

    private val mutex = Mutex()

    val downloadManagerPageEvent = EventHandler<DownloadManagerPageEvent>()
    val loadManagerPageEvent = EventHandler<LoadManagerPageEvent>()

    private val pageStatus = SparseArray<PageStatus>()
    private val pages = SparseArray<List<Post>>()
    val posts = EventCollection<Post>(ArrayList())

    val currentPost: Post? get() = posts[position]
    var currentPage = 0
        private set
    var position = -1

    suspend fun downloadPage(page: Int): List<Post> {
        return withContext(Dispatchers.Default) {
            val status = pageStatus[page]
            var p = pages[page]
            if (status == null) {
                pageStatus.append(page, PageStatus.DOWNLOADING)
                p = Api.getPosts(page, tags).filterNewPosts()
                pages.append(page, p)
                downloadManagerPageEvent.trigger(DownloadManagerPageEvent(page, p))
            } else
                if (status == PageStatus.DOWNLOADING) p = awaitPage(page)
            pageStatus.append(page, PageStatus.DOWNLOADED)
            return@withContext p
        }
    }

    suspend fun loadPage(context: Context, page: Int): List<Post>? {
        val p = pages[page]
        mutex.withLock {
            if (pageStatus[page] == PageStatus.DOWNLOADED && p != null) {
                if (page > currentPage) {
                    currentPage = page
                    posts += p
                }
                pageStatus.append(page, PageStatus.LOADED)
                withContext(Dispatchers.Main) {
                    loadManagerPageEvent.trigger(LoadManagerPageEvent(context, p))
                }
            }
        }
        return p
    }

    private suspend fun awaitPage(page: Int): List<Post> {
        var list: List<Post>? = null
        downloadManagerPageEvent.registerListener {
            if (it.page == page) {
                list = it.posts
                it.deleteListener = true
            }
        }
        return withContext(Dispatchers.IO) {
            while (list == null)
                delay(50)
            list!!
        }
    }

    private fun List<Post>.filterNewPosts(): List<Post> {
        val lastPostID = if (posts.isNotEmpty()) posts.last().id else Integer.MAX_VALUE
        return this.takeLastWhile { lastPostID > it.id }
    }

    suspend fun reset() {
        pageStatus.clear()
        pages.clear()
        position = -1
        currentPage = 0
        withContext(Dispatchers.Main) { posts.clear() }
    }

    override fun equals(other: Any?): Boolean {
        return if (other != null && other is Manager)
            tags.toTagString() == other.tags.toTagString()
        else false
    }
}

class DownloadManagerPageEvent(val page: Int, val posts: List<Post>) : Event()
class LoadManagerPageEvent(val context: Context, val newPage: List<Post>) : Event()

private enum class PageStatus {
    DOWNLOADING, DOWNLOADED, LOADED
}