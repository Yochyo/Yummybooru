package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.AddServerEvent

class DisplayToastAddServerListener : Listener<AddServerEvent> {
    override fun onEvent(e: AddServerEvent): Boolean {
        Toast.makeText(e.context, "Add server [${e.server.name}]", Toast.LENGTH_SHORT).show()
        return true
    }
}