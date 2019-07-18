package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.AddSubEvent

class DisplayToastAddSubListener : Listener<AddSubEvent> {
    override fun onEvent(e: AddSubEvent): Boolean {
        Toast.makeText(e.context, "Add ${if (e.sub.isFavorite) "favorite" else ""} sub [${e.sub.name}]", Toast.LENGTH_SHORT).show()
        return true
    }
}