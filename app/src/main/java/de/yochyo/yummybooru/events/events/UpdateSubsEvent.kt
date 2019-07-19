package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.entities.Subscription

class UpdateSubsEvent(val context: Context, val subs: Collection<Subscription>): Event{
    companion object: EventHandler<UpdateSubsEvent>()
}