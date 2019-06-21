package de.yochyo.ybooru.events.events

import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.api.downloads.Manager

class DownloadManagerPageEvent(val manager: Manager, val page: Int, val posts: List<Post>) : Event{
    companion object: EventHandler<DownloadManagerPageEvent>()
}