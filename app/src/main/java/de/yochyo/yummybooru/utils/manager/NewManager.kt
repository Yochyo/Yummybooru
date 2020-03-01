package de.yochyo.yummybooru.utils.manager

import android.content.Context
import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.api.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.max

class NewManager(val tags: String): IManager {
    private val mutex = Mutex()

    override val posts = EventCollection<Post>(ArrayList())
    override val onDownloadPageEvent = EventHandler<OnDownloadPageEvent>()

    var currentPage = 1
        private set

    /**
     * @return returns null on networks error, empty list on end
     */
    override suspend fun downloadNextPage(context: Context, limit: Int): List<Post>? {
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                var result: List<Post>? = null
                val page = Api.getPosts(context, currentPage++, tags.split(" ").toTypedArray(), limit)
                if (page != null) {
                    val newPosts = page.filterNewPosts()
                    posts += newPosts
                    result = newPosts
                }
                onDownloadPageEvent.trigger(OnDownloadPageEvent(result))
                result
            }
        }
    }
    private fun List<Post>.filterNewPosts(): List<Post> {
        val lastPostID = if (posts.isNotEmpty()) posts.last().id else Integer.MAX_VALUE
        return this.takeLastWhile { lastPostID > it.id }
    }

    override suspend fun clear() {
        mutex.withLock {
            posts.clear()
            currentPage = 1
        }
    }

    override fun toString(): String = tags
}