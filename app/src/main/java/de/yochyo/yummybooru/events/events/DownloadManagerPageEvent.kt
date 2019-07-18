package de.yochyo.yummybooru.events.events

import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.downloads.Manager

class DownloadManagerPageEvent(val manager: Manager, val page: Int, val posts: List<de.yochyo.yummybooru.api.Post>) : Event {
    companion object : EventHandler<DownloadManagerPageEvent>()
}