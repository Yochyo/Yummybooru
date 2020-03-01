package de.yochyo.yummybooru.utils.manager

import de.yochyo.eventmanager.Event
import de.yochyo.yummybooru.api.Post

class OnDownloadPageEvent(val page: Collection<Post>?) : Event()