package de.yochyo.yummybooru.api.manager

import android.content.Context
import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.manager.IManager
import de.yochyo.booruapi.manager.ManagerBuilder
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.yummybooru.database.db

open class ManagerWrapper(private val manager: IManager) : IManager by manager {
    val currentPost: Post?
        get() {
            return if (position in 0 until posts.size) posts[position]
            else null
        }
    var position = -1

    var reachedLastPage = false

    override suspend fun downloadNextPages(amount: Int): List<Post>? {
        val result = manager.downloadNextPages(amount)
        if (result != null && result.isEmpty()) posts.triggerOnAddElementsEvent(OnAddElementsEvent(posts, emptyList()))
        reachedLastPage = result?.isEmpty() == true
        return result
    }

    override fun toString() = manager.toString()

    companion object {
        fun build(context: Context, s: String) = ManagerWrapper(ManagerBuilder.createManager(context.db.currentServer.api, s, context.db.limit))
    }
}