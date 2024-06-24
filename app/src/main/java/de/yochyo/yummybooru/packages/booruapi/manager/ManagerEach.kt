package de.yochyo.booruapi.manager

import de.yochyo.booruapi.api.Post
import de.yochyo.eventcollection.EventCollection

class ManagerEach : IManager {
    companion object {
        val delimiters = ArrayList<String>().apply { add("OR"); add("THEN") }
    }

    override val limit: Int = 0
    override val posts: EventCollection<Post> = EventCollection(ArrayList())

    override suspend fun downloadNextPages(amount: Int): List<Post>? {
        return null
    }

    override suspend fun clear() {}
}