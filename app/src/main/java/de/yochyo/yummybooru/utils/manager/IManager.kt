package de.yochyo.yummybooru.utils.manager

import android.content.Context
import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Post

interface IManager {
    val posts: EventCollection<Post>
    val onDownloadPageEvent: EventHandler<OnDownloadPageEvent>

    suspend fun downloadNextPage(context: Context, limit: Int): List<Post>?
    suspend fun clear()
}