package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.SelectServerEvent

class DisplayToastSelectServerListener : Listener<SelectServerEvent>{
    override fun onEvent(e: SelectServerEvent): Boolean {
        Toast.makeText(e.context, "Selected server [${e.newServer.name}]", Toast.LENGTH_SHORT).show()
        return true
    }
}