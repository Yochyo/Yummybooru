package de.yochyo.yummybooru.events.events

import android.content.Context
import android.support.v4.provider.DocumentFile
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Post
import java.io.File

class SafeFileEvent(val context: Context, val file: DocumentFile, val post: Post, val source: Int = DEFAULT) : Event{
    companion object: EventHandler<SafeFileEvent>(){
        val DEFAULT = 0
        val SILENT = 99
    }
}