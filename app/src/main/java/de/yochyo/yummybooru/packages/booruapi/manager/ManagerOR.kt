package de.yochyo.booruapi.manager

import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.utils.removeDuplicatesUpdateCachedList
import de.yochyo.eventcollection.EventCollection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList
import java.util.TreeSet
import kotlin.math.min

class ManagerOR(val managers: Collection<IManager>, override val limit: Int) : IManager {
    private val mutex = Mutex()

    override val posts: EventCollection<Post> = EventCollection(ArrayList())
    private val cachedPostIds = TreeSet<Int>()

    private val cachedPosts = LinkedList<Post>()

    private var sortingAlgorithm: IManagerORSortingAlgorithm? = null

    companion object {
        var defaultSortingAlgerithm: IManagerORSortingAlgorithm = ManagerORDefaultSortingAlgorithm()
    }

    override suspend fun downloadNextPages(amount: Int): List<Post>? {
        return mutex.withLock {
            while (cachedPosts.size < limit * amount) {
                val pages = managers.map { it.downloadNextPages(amount) }
                val pagesNotNull = pages.mapNotNull { it }
                if (pagesNotNull.isEmpty()) return@withLock null

                if (pagesNotNull.isEmpty() || pagesNotNull.flatten().isEmpty()) break
                cachedPosts += getSortingAlgorithm().mergePages(pagesNotNull)
            }
            val posts = removeDuplicatesUpdateCachedList(cachedPostIds, takeLastCached(limit * amount))
            this.posts += posts
            posts
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            for (m in managers) m.clear()
            posts.clear()
            cachedPosts.clear()
            cachedPostIds.clear()
        }
    }

    override fun toString(): String {
        return managers.joinToString(" OR ") { it.toString() }
    }

    private fun takeLastCached(size: Int): List<Post> {
        val list = LinkedList<Post>()
        for (i in 0 until min(cachedPosts.size, size)) {
            list += cachedPosts.remove()
        }
        return list
    }

    private fun getSortingAlgorithm(): IManagerORSortingAlgorithm = sortingAlgorithm ?: defaultSortingAlgerithm
}

interface IManagerORSortingAlgorithm {
    fun mergePages(pages: List<List<Post>>): List<Post>
}

class ManagerORDefaultSortingAlgorithm() : IManagerORSortingAlgorithm {
    override fun mergePages(pages: List<List<Post>>): List<Post> {
        return pages.flatten()
    }
}

class ManagerORRandomSortingAlgorithm() : IManagerORSortingAlgorithm {
    override fun mergePages(pages: List<List<Post>>): List<Post> {
        return pages.flatten().shuffled()
    }
}

class ManagerORAlternatingAlgorithm(val steps: Int) : IManagerORSortingAlgorithm {
    init {
        if (steps < 1) throw Exception("steps cannot be smaller than 1")
    }

    override fun mergePages(pages: List<List<Post>>): List<Post> {
        val result = LinkedList<Post>()
        val chunks = pages.map { it.chunked(steps) }
        val max = chunks.map { it.size }.maxOrNull() ?: return emptyList()
        for (i in 0 until max)
            chunks.forEach { chunkList ->
                val chunk = chunkList.getOrNull(i)
                if (chunk != null) result += chunk
            }
        return result
    }
}