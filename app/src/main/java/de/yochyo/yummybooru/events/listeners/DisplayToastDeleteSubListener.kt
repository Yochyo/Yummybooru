package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.DeleteSubEvent

class DisplayToastDeleteSubListener : Listener<DeleteSubEvent>() {
    override fun onEvent(e: DeleteSubEvent) {
        Toast.makeText(e.context, "Delete sub [${e.sub.name}]", Toast.LENGTH_SHORT).show()
    }
}