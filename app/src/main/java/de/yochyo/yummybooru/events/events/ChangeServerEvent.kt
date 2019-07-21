package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.entities.Server

class ChangeServerEvent(val context: Context, val oldServer: Server, val newServer: Server) : Event() {
    companion object : EventHandler<ChangeServerEvent>()
}