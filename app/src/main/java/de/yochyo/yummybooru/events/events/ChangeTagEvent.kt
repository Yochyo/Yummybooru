package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.entities.Tag

class ChangeTagEvent(val context: Context, val oldTag: Tag, val newTag: Tag) : Event{
    companion object: EventHandler<ChangeTagEvent>()
}