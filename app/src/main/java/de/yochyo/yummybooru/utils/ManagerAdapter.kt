package de.yochyo.yummybooru.utils

import android.content.Context
import de.yochyo.booruapi.manager.IManager
import de.yochyo.booruapi.manager.ManagerBuilder
import de.yochyo.booruapi.objects.Post
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.currentServer

class ManagerWrapper(private val manager: IManager) : IManager by manager {
    val currentPost: Post?
        get() {
            return if (position in 0 until posts.size) posts[position]
            else null
        }
    var position = -1

    override fun toString() = manager.toString()

    //This will let the activities know that the end was reached
    override suspend fun downloadNextPage() = downloadNextPages(1)
    //This will let the activities know that the end was reached
    override suspend fun downloadNextPages(amount: Int): List<Post>? {
        val result = manager.downloadNextPages(amount)
        if (result != null && result.isEmpty()) posts.triggerOnAddElementsEvent(OnAddElementsEvent(posts, emptyList()))
        return result
    }

    companion object {
        fun build(context: Context, s: String) = ManagerWrapper(ManagerBuilder.toManager(context.currentServer.api, s, context.db.limit))
    }
}