package de.yochyo.yummybooru.api.manager

import android.content.Context
import de.yochyo.booruapi.manager.IManager
import de.yochyo.booruapi.manager.ManagerBuilder
import de.yochyo.booruapi.objects.Post
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.yummybooru.database.db

class ManagerWrapper(private val manager: IManager) : IManager by manager {
    val currentPost: Post?
        get() {
            return if (position in 0 until posts.size) posts[position]
            else null
        }
    var position = -1

    override fun toString() = manager.toString()

    private var bool = true
    override suspend fun downloadNextPage(): List<Post>? {
        if (bool) {
            bool = false
            return downloadNextPages(1)
        }
        return null
    }

    override suspend fun downloadNextPages(amount: Int): List<Post>? {
        val result = manager.downloadNextPages(amount)
        if (result != null && result.isEmpty()) posts.triggerOnAddElementsEvent(OnAddElementsEvent(posts, emptyList()))
        return result
    }

    companion object {
        fun build(context: Context, s: String) = ManagerWrapper(ManagerBuilder.toManager(context.db.currentServer.api, s, context.db.limit))
    }
}