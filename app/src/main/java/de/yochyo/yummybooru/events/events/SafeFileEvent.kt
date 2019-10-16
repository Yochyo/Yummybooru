package de.yochyo.yummybooru.events.events

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Post

class SafeFileEvent(val context: Context, val file: DocumentFile, val post: Post, val source: Int = DEFAULT) : Event() {
    companion object : EventHandler<SafeFileEvent>() {
        const val DEFAULT = 0
        const val SILENT = 99
    }
}