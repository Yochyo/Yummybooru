package de.yochyo.booruapi.manager

import de.yochyo.booruapi.api.Post
import de.yochyo.eventcollection.EventCollection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ManagerThen(val managers: List<IManager>) : IManager {

    init {
        require(managers.isNotEmpty())
    }

    override val limit: Int = managers.maxOf { it.limit }

    private val mutex = Mutex()
    private var currentManagerIndex = 0

    override val posts: EventCollection<Post> = EventCollection(ArrayList())

    val currentManager get() = managers[currentManagerIndex]
    val reachedEnd get() = currentManagerIndex == managers.size

    override suspend fun downloadNextPages(amount: Int): List<Post>? = mutex.withLock { downloadNextPagesRec(amount) }

    private suspend fun downloadNextPagesRec(amount: Int): List<Post>? {
        if (reachedEnd) return emptyList()

        val pages = currentManager.downloadNextPages(amount) ?: return null
        return if (pages.isEmpty()) {
            currentManagerIndex++
            downloadNextPagesRec(amount)
        } else pages
    }

    override suspend fun clear() {
        mutex.withLock {
            currentManagerIndex = 0
            posts.clear()
            managers.forEach { it.clear() }
        }
    }

    override fun toString(): String {
        return managers.joinToString(" THEN ") { it.toString() }
    }
}