package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.entities.Tag

class DeleteTagEvent(val context: Context,val tag: Tag) : Event{
    companion object: EventHandler<DeleteTagEvent>()
}