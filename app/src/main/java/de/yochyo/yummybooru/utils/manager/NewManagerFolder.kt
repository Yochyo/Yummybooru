package de.yochyo.yummybooru.utils.manager

import android.content.Context
import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class NewManagerFolder(val managers: Collection<IManager>) : IManager {
    private val mutex = Mutex()

    override val posts: EventCollection<Post> = EventCollection(ArrayList())
    override val onDownloadPageEvent = EventHandler<OnDownloadPageEvent>()

    /**
     * this TreeSet stores values of all Posts. It is used to search for double posts faster
     */
    private val postsSet = TreeSet<Post>()
    /**
     * All posts not yet in posts
     */
    private val bufferedPosts = LinkedList<Post>()

    override suspend fun downloadNextPage(context: Context, limit: Int): List<Post>? {
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                while (bufferedPosts.size < limit) {
                    val result = TreeSet<Post>()
                    for (m in managers.iterator()) {
                        val r = m.downloadNextPage(context, limit)
                        if (r != null) result += r
                        else return@withContext null
                    }
                    if (result.isEmpty()) break //In case manager arrived at the end
                    val filteredResult = result.filter { !postsSet.contains(it) }
                    postsSet += filteredResult
                    bufferedPosts += filteredResult
                }
                val page = ArrayList<Post>(limit)
                try{
                    for (i in 0 until limit)
                        page += bufferedPosts.removeLast()
                }catch (e: Exception){}
                posts += page
                onDownloadPageEvent.trigger(OnDownloadPageEvent(page))
                page
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            for (m in managers) m.clear()
            posts.clear()
            bufferedPosts.clear()
            postsSet.clear()
        }
    }

    override fun toString(): String {
        return managers.joinToString(" OR ") { it.toString() }
    }
}