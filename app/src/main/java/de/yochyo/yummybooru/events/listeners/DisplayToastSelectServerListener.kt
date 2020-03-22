package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.SelectServerEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DisplayToastSelectServerListener : Listener<SelectServerEvent>() {
    private var firstTrigger = true
    override fun onEvent(e: SelectServerEvent) {
        if(firstTrigger) firstTrigger = false
        else GlobalScope.launch(Dispatchers.Main) { Toast.makeText(e.context, "Selected server [${e.newServer.name}]", Toast.LENGTH_SHORT).show() }
    }
}