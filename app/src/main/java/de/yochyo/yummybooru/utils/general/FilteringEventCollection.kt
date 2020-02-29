package de.yochyo.yummybooru.utils.general

import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventcollection.SubEventCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class FilteringEventCollection<E>(private val getUnfilteredCollection: () -> EventCollection<E>, private val filterBy: (e: E) -> String) : Collection<E> {
    private val eventCollections = ArrayList<Pair<EventCollection<E>, String>>()
    private val filterMutex = Mutex()

    private val currentFilter: EventCollection<E> get() = eventCollections.last().first

    init {
        eventCollections += Pair(getUnfilteredCollection(), "")
    }

    suspend fun filter(name: String): EventCollection<E> {
        filterMutex.withLock {
            withContext(Dispatchers.Default) {
                var result: EventCollection<E>? = null
                if (name == "") {
                    clear()
                    result = getUnfilteredCollection()
                } else {
                    for (i in eventCollections.indices.reversed()) {
                        if (name.startsWith(eventCollections[i].second)) {
                            result = SubEventCollection(TreeSet(), eventCollections[i].first) { filterBy(it).contains(name) }
                            break
                        }
                    }
                }
                if (result == null) result = SubEventCollection(TreeSet(), getUnfilteredCollection()) { filterBy(it).contains(name) }
                eventCollections += Pair(result, name)
            }
        }
        return currentFilter
    }

    fun clear() {
        for (item in eventCollections) {
            val list = item.first
            if (list is SubEventCollection) list.destroy()
        }
        eventCollections.clear()
    }

    override val size: Int
        get() = currentFilter.size

    override fun contains(element: E) = currentFilter.contains(element)

    override fun containsAll(elements: Collection<E>) = currentFilter.containsAll(elements)

    override fun isEmpty(): Boolean = false

    override fun iterator(): Iterator<E> = currentFilter.iterator()
}