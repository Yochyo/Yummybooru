package de.yochyo.booruapi.manager

import de.yochyo.booruapi.api.Post
import de.yochyo.eventcollection.EventCollection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ManagerNOT(private val manager: IManager, private val excluding: Collection<String>) : IManager by manager {
    override val posts: EventCollection<Post> = EventCollection(ArrayList())
    private val mutex = Mutex()

    override suspend fun downloadNextPages(amount: Int): List<Post>? {
        return mutex.withLock {
            val pages = manager.downloadNextPages(amount) ?: return@withLock null
            val filtered = pages.filter {
                for (ex in excluding) if (it.tagString.contains(" $ex "))
                    return@filter false
                true
            }

            //return null (error) if everything was filtered
            if (pages.isNotEmpty() && filtered.isEmpty()) return null
            posts += filtered
            filtered
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            posts.clear()
            manager.clear()
        }
    }

    override fun toString(): String {
        return "$manager ${excluding.joinToString(" ") { "NOT($it)" }}"
    }
}