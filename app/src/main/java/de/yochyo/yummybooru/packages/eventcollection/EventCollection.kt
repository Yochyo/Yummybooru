package de.yochyo.eventcollection

import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventcollection.events.OnClearEvent
import de.yochyo.eventcollection.events.OnRemoveElementsEvent
import de.yochyo.eventcollection.events.OnReplaceCollectionEvent
import de.yochyo.eventcollection.events.OnReplaceElementEvent
import de.yochyo.eventcollection.events.OnUpdateEvent
import de.yochyo.eventmanager.EventHandler
import java.util.LinkedList
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * An EventCollection is a collection that triggers events on changes.
 * Uses for it are for example GUI's that work with a changing list
 *
 * @param collection the collection the data is saved into. Depending on your use case, any MutableCollection can be used.
 * This variable is public BUT should not be used if you don't know what you're code is doing or will do, as changes
 * will not trigger events
 * @param T type of elements contained in the collection
 *
 * @property onUpdate triggers an event when the collection is changed
 * @property onClear triggers an event when the collection is cleared
 * @property onAddElements triggers an event when an element is added to the collection
 * @property onRemoveElements triggers an event when an element is removed from the collection
 */
open class EventCollection<T>(override var collection: MutableCollection<T>) : IEventCollection<T> {

    override val onUpdate = EventHandler<OnUpdateEvent<T>>()
    override val onClear = object : EventHandler<OnClearEvent<T>>() {
        override fun trigger(e: OnClearEvent<T>) {
            super.trigger(e)
            notifyChange()
        }
    }
    override val onAddElements = object : EventHandler<OnAddElementsEvent<T>>() {
        override fun trigger(e: OnAddElementsEvent<T>) {
            super.trigger(e)
            notifyChange()
        }
    }
    override val onRemoveElements = object : EventHandler<OnRemoveElementsEvent<T>>() {
        override fun trigger(e: OnRemoveElementsEvent<T>) {
            super.trigger(e)
            notifyChange()
        }
    }
    override val onReplaceCollection = object : EventHandler<OnReplaceCollectionEvent<T>>() {
        override fun trigger(e: OnReplaceCollectionEvent<T>) {
            super.trigger(e)
            notifyChange()
        }
    }
    override val onReplaceElement = object : EventHandler<OnReplaceElementEvent<T>>() {
        override fun trigger(e: OnReplaceElementEvent<T>) {
            super.trigger(e)
            notifyChange()
        }
    }

    override fun replaceCollection(c: MutableCollection<T>) {
        val old = collection
        collection = c
        onReplaceCollection.trigger(OnReplaceCollectionEvent(old, c))
    }

    override fun replaceElement(old: T, new: T): Boolean {
        if (collection.remove(old) && collection.add(new)) {
            onReplaceElement.trigger(OnReplaceElementEvent(old, new, collection))
            return true
        }

        return false
    }

    override val size: Int get() = collection.size
    override fun contains(element: T) = collection.contains(element)
    override fun containsAll(elements: Collection<T>) = collection.containsAll(elements)
    override fun isEmpty() = collection.isEmpty()
    operator fun get(index: Int) = collection.elementAt(index)

    override fun add(element: T): Boolean {
        val res = collection.add(element)
        if (res)
            onAddElements.trigger(OnAddElementsEvent(collection, listOf(element)))
        return res
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val added = LinkedList<T>()
        for (element in elements) if (collection.add(element)) added += element
        if (added.isNotEmpty()) onAddElements.trigger(OnAddElementsEvent(collection, added))
        return added.isNotEmpty()
    }

    override fun remove(element: T): Boolean {
        val res = collection.remove(element)
        if (res)
            onRemoveElements.trigger(OnRemoveElementsEvent(collection, listOf(element)))
        return res
    }

    override fun clear() {
        collection.clear()
        onClear.trigger(OnClearEvent(collection))
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val removed = LinkedList<T>()
        for (e in elements) if (collection.remove(e)) removed += e
        if (removed.isNotEmpty()) onRemoveElements.trigger(OnRemoveElementsEvent(collection, removed))
        return removed.isNotEmpty()
    }

    override fun removeIf(filter: Predicate<in T>): Boolean {
        val removed = LinkedList<T>()
        val iter = iterator()
        while (iter.hasNext()) {
            val current = iter.next()
            if (filter.test(current)) {
                removed += current
                iter.remove()
            }
        }
        if (removed.isNotEmpty()) onRemoveElements.trigger(OnRemoveElementsEvent(collection, removed))
        return removed.isNotEmpty()
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val iter = collection.iterator()
        val removed = LinkedList<T>()
        while (iter.hasNext()) {
            val current = iter.next()
            if (!elements.contains(current)) {
                removed += current
                iter.remove()
            }
        }
        if (removed.isNotEmpty()) onRemoveElements.trigger(OnRemoveElementsEvent(collection, removed))
        return removed.isNotEmpty()
    }

    override fun iterator(): MutableIterator<T> = getIterator(collection)

    protected fun getIterator(c: MutableCollection<T>): MutableIterator<T> {
        val i = c.iterator()
        return object : MutableIterator<T> {
            private var current: T? = null
            override fun hasNext() = i.hasNext()
            override fun next(): T {
                val next = i.next()
                current = next
                return next
            }

            override fun remove() {
                i.remove()
                val element = current
                if (element != null)
                    onRemoveElements.trigger(OnRemoveElementsEvent(collection, listOf(element)))
            }
        }
    }

    override fun stream() = collection.stream()
    override fun forEach(action: Consumer<in T>?) = collection.forEach(action)


    @Deprecated("Will not trigger events")
    override fun parallelStream() = collection.parallelStream()

    @Deprecated("Will not trigger events")
    override fun spliterator() = collection.spliterator()
}
