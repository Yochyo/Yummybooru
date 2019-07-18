package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.entities.Subscription

class ChangeSubEvent(val context: Context, val oldSub: Subscription, val newSub: Subscription) : Event {
    companion object : EventHandler<ChangeSubEvent>()
}