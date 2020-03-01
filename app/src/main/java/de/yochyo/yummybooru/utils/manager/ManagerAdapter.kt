package de.yochyo.yummybooru.utils.manager

import android.content.Context
import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Post

class ManagerWrapper(private val manager: IManager) : IManager {
    val currentPost: Post? get(){
        if(position in 0 until posts.size) return posts[position]
        else return null
    }
    var position = -1

    override val onDownloadPageEvent: EventHandler<OnDownloadPageEvent>
        get() = manager.onDownloadPageEvent
    override val posts: EventCollection<Post>
        get() = manager.posts

    override suspend fun clear() {
        manager.clear()
    }

    override suspend fun downloadNextPage(context: Context, limit: Int): List<Post>? {
        return manager.downloadNextPage(context, limit)
    }

    override fun toString(): String {
        return manager.toString()
    }
}