package de.yochyo.yummybooru.utils.general

import de.yochyo.eventcollection.IEventCollection
import de.yochyo.eventcollection.SubEventCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class FilteringEventCollection<E>(
    private val getUnfilteredCollection: () -> IEventCollection<E>,
    private val filterBy: (e: E) -> String,
    private val createCollection: () -> MutableCollection<E> = { TreeSet() }
) : Collection<E> {
    private val eventCollections = ArrayList<Pair<IEventCollection<E>, String>>()

    private fun getLatestEventCollection(): IEventCollection<E> {
        if (eventCollections.isEmpty()) eventCollections += Pair(getUnfilteredCollection(), "")
        return eventCollections.last().first
    }

    private val filterMutex = Mutex()

    suspend fun filter(name: String): IEventCollection<E> {
        filterMutex.withLock {
            withContext(Dispatchers.Default) {
                var result: IEventCollection<E>? = null
                if (name == "") {
                    clear()
                    result = getUnfilteredCollection()
                } else {
                    for (i in eventCollections.indices.reversed()) {
                        if (name.startsWith(eventCollections[i].second)) {
                            result = SubEventCollection(createCollection(), eventCollections[i].first) { filterBy(it).contains(name, true) }
                            break
                        }
                    }
                }
                if (result == null) result = SubEventCollection(createCollection(), getUnfilteredCollection()) { filterBy(it).contains(name, true) }
                eventCollections += Pair(result, name)
            }
        }
        return getLatestEventCollection()
    }

    fun clear() {
        for (item in eventCollections) {
            val list = item.first
            if (list is SubEventCollection<*>) list.close()
        }
        eventCollections.clear()
        eventCollections += Pair(getUnfilteredCollection(), "")
    }

    override val size: Int
        get() = getLatestEventCollection().size

    override fun contains(element: E) = getLatestEventCollection().contains(element)

    override fun containsAll(elements: Collection<E>) = getLatestEventCollection().containsAll(elements)

    override fun isEmpty(): Boolean = false

    override fun iterator(): Iterator<E> = getLatestEventCollection().iterator()
}