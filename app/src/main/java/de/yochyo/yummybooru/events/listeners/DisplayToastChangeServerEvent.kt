package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.ChangeServerEvent

class DisplayToastChangeServerEvent : Listener<ChangeServerEvent> {
    override fun onEvent(e: ChangeServerEvent) {
        if (e.oldServer == e.newServer)
            Toast.makeText(e.context, "Edited [${e.newServer.name}]", Toast.LENGTH_SHORT).show()
    }
}