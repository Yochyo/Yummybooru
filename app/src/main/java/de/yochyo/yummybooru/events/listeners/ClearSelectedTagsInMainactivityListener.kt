package de.yochyo.yummybooru.events.listeners

import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.layout.MainActivity

class ClearSelectedTagsInMainactivityListener : Listener<SelectServerEvent> {
    override fun onEvent(e: SelectServerEvent) {
        if (e.oldServer != e.newServer)
            MainActivity.selectedTags.clear()
    }
}