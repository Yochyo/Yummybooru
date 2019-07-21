package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.entities.Subscription

class AddSubEvent(val context: Context, val sub: Subscription) : Event() {
    companion object : EventHandler<AddSubEvent>()
}