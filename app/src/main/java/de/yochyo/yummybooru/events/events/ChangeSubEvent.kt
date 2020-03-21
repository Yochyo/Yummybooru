package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.entities.Tag

class ChangeSubEvent(val context: Context, val oldSub: Tag, val newSub: Tag) : Event() {
    companion object : EventHandler<ChangeSubEvent>()
}