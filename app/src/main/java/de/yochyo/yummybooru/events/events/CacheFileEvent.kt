package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Post
import java.io.File

class CacheFileEvent(val context: Context, val file: File, val post: Post) : Event() {
    companion object : EventHandler<CacheFileEvent>()
}