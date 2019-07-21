package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.entities.Tag

class UpdateTagsEvent(val context: Context, val tags: Collection<Tag>): Event(){
    companion object: EventHandler<UpdateTagsEvent>()
}