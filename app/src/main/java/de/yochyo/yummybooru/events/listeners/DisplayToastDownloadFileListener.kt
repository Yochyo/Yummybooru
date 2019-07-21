package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.SafeFileEvent

class DisplayToastDownloadFileListener : Listener<SafeFileEvent> {
    override fun onEvent(e: SafeFileEvent) {
        if (e.source == SafeFileEvent.DEFAULT)
            Toast.makeText(e.context, "Download ${e.post.id}", Toast.LENGTH_SHORT).show()
    }
}