package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.DeleteServerEvent

class DisplayToastDeleteServerListener : Listener<DeleteServerEvent> {
    override fun onEvent(e: DeleteServerEvent): Boolean {
        Toast.makeText(e.context, "Deleted server [${e.server.name}]", Toast.LENGTH_SHORT).show()
        return true
    }
}