package de.yochyo.booruapi.manager

import de.yochyo.booruapi.api.IBooruApi
import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.utils.removeDuplicatesUpdateCachedList
import de.yochyo.eventcollection.EventCollection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList
import java.util.TreeSet

class Manager(val api: IBooruApi, val tags: String, override val limit: Int) : IManager {
    private var reachedLastPage = false
    private val mutex = Mutex()

    override val posts = EventCollection<Post>(ArrayList())
    private val cachedPostIds = TreeSet<Int>()


    var currentPage = 1
        private set

    override suspend fun downloadNextPages(amount: Int): List<Post>? {
        return mutex.withLock {
            if (reachedLastPage) emptyList()
            else {
                val res = _downloadNextPages(amount)
                if (res?.isEmpty() == true) reachedLastPage = true
                res
            }
        }
    }

    private suspend fun _downloadNextPages(amount: Int): List<Post>? {
        val time = System.currentTimeMillis()
        val pages = (currentPage until currentPage + amount).map {
            GlobalScope.async { api.getPosts(it, tags, limit) }
        }.awaitAll()
        println("Manager: ${System.currentTimeMillis() - time}ms, $tags, $amount page(s), limit $limit, ${api.host}")
        //If an error occured while downloading a page, all pages after the error are scrapped by only taking the pages before
        val pagesUntilError = pages.takeWhile { it != null }.mapNotNull { it }
        if (pagesUntilError.isEmpty()) return null

        currentPage += pagesUntilError.size

        val allPagesAsList = LinkedList<Post>().apply { pagesUntilError.forEach { this += it } }
        val resultWithoutDuplicates = removeDuplicatesUpdateCachedList(cachedPostIds, allPagesAsList)
        posts += resultWithoutDuplicates
        return resultWithoutDuplicates
    }

    override suspend fun clear() {
        mutex.withLock {
            posts.clear()
            cachedPostIds.clear()
            reachedLastPage = false
            currentPage = 1
        }
    }

    override fun toString(): String = tags
}