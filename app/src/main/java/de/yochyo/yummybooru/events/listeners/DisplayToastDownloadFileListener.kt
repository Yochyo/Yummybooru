package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.events.events.SafeFileEvent

class DisplayToastDownloadFileListener : Listener<SafeFileEvent>() {
    override fun onEvent(e: SafeFileEvent) {
        if (e.source == SafeFileEvent.DEFAULT)
            Toast.makeText(e.context, e.context.getString(R.string.download_post_with_id, e.post.id), Toast.LENGTH_SHORT).show()
    }
}