package de.yochyo.booruapi.manager

import de.yochyo.booruapi.api.Post
import de.yochyo.eventcollection.EventCollection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min

class BufferedManager(private val manager: IManager) : IManager by manager {
    override val posts: EventCollection<Post> = EventCollection(ArrayList())

    private val bufferedPosts = LinkedList<Post>()
    private val mutex = Mutex()

    private var totalAmountOfPages = 0
    override suspend fun downloadNextPages(amount: Int): List<Post>? {
        return mutex.withLock {
            while (bufferedPosts.size < limit * amount) {
                val guessedPageAmount = guessPagesForSinglePageDownload() * amount
                val page = manager.downloadNextPages(guessedPageAmount) ?: return@withLock null
                if (page.isEmpty()) break

                totalAmountOfPages += guessedPageAmount
                bufferedPosts += page
            }

            val result = take(limit * amount)
            posts += result

            result
        }
    }

    private fun guessPagesForSinglePageDownload(): Int {
        if (posts.size <= 0) return 1
        return max(1, min(totalAmountOfPages * limit / posts.size, 5))
    }

    override suspend fun downloadNextPage(): List<Post>? {
        return downloadNextPages(1)
    }

    private fun take(n: Int): List<Post> {
        val result = LinkedList<Post>()
        for (i in 0 until min(n, bufferedPosts.size)) result += bufferedPosts.removeFirst()
        return result
    }

    override suspend fun clear() {
        mutex.withLock {
            posts.clear()
            manager.clear()
            bufferedPosts.clear()
        }
    }

    override fun toString(): String {
        return manager.toString()
    }
}