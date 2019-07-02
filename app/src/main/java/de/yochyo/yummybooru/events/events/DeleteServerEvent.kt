package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.entities.Server

class DeleteServerEvent(val context: Context, val server: Server) : Event{
    companion object: EventHandler<DeleteServerEvent>()
}