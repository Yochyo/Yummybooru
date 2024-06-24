package de.yochyo.booruapi.manager

import de.yochyo.booruapi.api.Post
import de.yochyo.eventcollection.EventCollection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ManagerWithIdLimit(private val manager: IManager, val minId: Int, override val limit: Int = manager.limit) : IManager {
    private val mutex = Mutex()
    override val posts = EventCollection<Post>(ArrayList())
    private var reachedEnd = false

    override suspend fun downloadNextPages(amount: Int): List<Post>? {
        return mutex.withLock {
            if (reachedEnd) return@withLock emptyList()

            val posts = manager.downloadNextPages(amount)?.filter { it.id > minId } ?: return null
            if (posts.isEmpty()) reachedEnd = true
            this.posts += posts
            posts
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            posts.clear()
            reachedEnd = false
            manager.clear()
        }
    }

    override fun toString(): String {
        return "id:>$minId $manager"
    }
}