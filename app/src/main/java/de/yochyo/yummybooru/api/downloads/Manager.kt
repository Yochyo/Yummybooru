package de.yochyo.yummybooru.api.downloads

import android.content.Context
import android.util.SparseArray
import de.yochyo.eventmanager.EventCollection
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.events.events.DownloadManagerPageEvent
import de.yochyo.yummybooru.events.events.LoadManagerPageEvent
import de.yochyo.yummybooru.utils.toTagString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

abstract class Manager(val tags: Array<String>) {
    companion object {
        private val lock = Any()
        private val managers = Stack<Manager>()
        fun peek(): Manager?{
            return try{
                managers.peek()
            }catch(e: java.lang.Exception){null}
        }
        fun pop(): Manager? {
            return try {
                managers.pop()
            } catch (e: Exception) {
                null
            }
        }

        fun push(tags: Array<String>) = managers.push(object: Manager(tags){})
    }

    val posts = EventCollection<Post>(ArrayList())
    private val pageStatus = SparseArray<PageStatus>()
    private val pages = SparseArray<List<Post>>()
    var position = -1
    var currentPage = 0
        private set(value) {
            field = value
        }
    val currentPost: Post?
        get() = posts[position]


    fun loadPage(context: Context, page: Int): List<Post>? {
        if (pageStatus[page] == PageStatus.DOWNLOADED) {
            val p = pages[page]
            if (p != null) {
                if (page > currentPage) {
                    currentPage = page
                    posts += p
                }
                LoadManagerPageEvent.trigger(LoadManagerPageEvent(context, this, p))
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
            if (posts.isNotEmpty()) {
                val lastFromLastPage = posts.last()
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
        DownloadManagerPageEvent.registerListener {
            if (it.manager == this && it.page == page) {
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

private enum class PageStatus {
    DOWNLOADING, DOWNLOADED
}