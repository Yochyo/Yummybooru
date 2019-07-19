package de.yochyo.yummybooru.events.events

import android.content.Context
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.downloads.Manager

class LoadManagerPageEvent(val context: Context, val manager: Manager, val newPage: List<Post>) : Event{
    companion object: EventHandler<LoadManagerPageEvent>()
}