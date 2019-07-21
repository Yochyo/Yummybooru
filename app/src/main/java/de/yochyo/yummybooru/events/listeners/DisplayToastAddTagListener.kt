package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.AddTagEvent

class DisplayToastAddTagListener : Listener<AddTagEvent> {
    override fun onEvent(e: AddTagEvent) {
        Toast.makeText(e.context, "Add ${if (e.tag.isFavorite) "favorite" else ""} tag [${e.tag.name}]", Toast.LENGTH_SHORT).show()
    }
}