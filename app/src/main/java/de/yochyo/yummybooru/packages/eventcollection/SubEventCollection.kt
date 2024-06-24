package de.yochyo.eventcollection

import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventcollection.events.OnClearEvent
import de.yochyo.eventcollection.events.OnRemoveElementsEvent
import de.yochyo.eventcollection.events.OnReplaceCollectionEvent
import de.yochyo.eventcollection.events.OnReplaceElementEvent
import de.yochyo.eventmanager.Listener
import java.io.Closeable
import java.util.LinkedList
import java.util.function.Predicate

/**
 * @see EventCollection
 * An EventCollection that contains all elements of it's parent EventCollection
 * which fulfil the condition that filter() return true.
 * All changes in it's parent are reflected in it.
 * All changes in it are reflected in the parent.
 * For example: If an element is added to the parent EventCollection, it will (if filter() returns true)
 * be added to the SubEventCollection. If an element is added to the parent, it will be added to the SubEventCollection
 *
 * ATTENTION: If you don't need it anymore, call destroy() to remove it's listeners from the parent EventCollection
 *
 * @param parentCollection the parent EventCollection which content should be reflected here
 * @param filter if filter() returns true, an element will be contained in here
 */
open class SubEventCollection<T>(c: MutableCollection<T>, val parentCollection: IEventCollection<T>, protected val filter: (e: T) -> Boolean) :
    EventCollection<T>(c), Closeable {
    override fun add(element: T) = parentCollection.add(element)
    override fun addAll(elements: Collection<T>) = parentCollection.addAll(elements)
    override fun remove(element: T) = parentCollection.remove(element)
    override fun clear() = parentCollection.clear()
    override fun removeAll(elements: Collection<T>) = parentCollection.removeAll(elements)
    override fun removeIf(filter: Predicate<in T>) = parentCollection.removeIf(filter)
    override fun retainAll(elements: Collection<T>) = parentCollection.retainAll(elements)
    override fun replaceCollection(c: MutableCollection<T>) = parentCollection.replaceCollection(c)

    private val onClearParent = Listener<OnClearEvent<T>> {
        collection.clear()
        onClear.trigger(OnClearEvent(collection))
    }
    private val onAddElementParent = Listener<OnAddElementsEvent<T>> {
        val add = LinkedList<T>()
        it.elements.forEach { element -> if (filter(element)) add += element }
        addToCollection(add)
    }
    private val onRemoveElementParent = Listener<OnRemoveElementsEvent<T>> {
        val remove = LinkedList<T>()
        it.elements.forEach { element -> if (filter(element)) remove += element }
        removeFromCollection(remove)
    }
    private val onReplaceCollectionListener = Listener<OnReplaceCollectionEvent<T>> {
        initSubCollection()
        onReplaceCollection.trigger(OnReplaceCollectionEvent(it.old, it.new))
    }
    private val onReplaceElementListener = Listener<OnReplaceElementEvent<T>> {
        if (filter(it.old)) {
            collection.remove(it.old)
            if (filter(it.element)) replaceInCollection(it.old, it.element)
        } else if (filter(it.element)) addToCollection(listOf(it.element))
    }

    protected fun replaceInCollection(old: T, element: T) {
        collection.add(element)
        onReplaceElement.trigger(OnReplaceElementEvent(old, element, collection))
    }

    protected fun addToCollection(elements: Collection<T>) {
        collection.addAll(elements)
        onAddElements.trigger(OnAddElementsEvent(collection, elements))
    }

    protected fun removeFromCollection(elements: Collection<T>) {
        collection.removeAll(elements)
        onRemoveElements.trigger(OnRemoveElementsEvent(collection, elements))
    }

    init {
        initSubCollection()

        parentCollection.registerOnClearListener(onClearParent)
        parentCollection.registerOnAddElementsListener(onAddElementParent)
        parentCollection.registerOnRemoveElementsListener(onRemoveElementParent)
        parentCollection.registerOnReplaceCollectionListener(onReplaceCollectionListener)
    }

    protected fun initSubCollection() {
        collection.clear()
        for (e in parentCollection)
            if (filter(e)) collection.add(e)
    }

    override fun close() {
        parentCollection.removeOnClearListener(onClearParent)
        parentCollection.removeOnAddElementsListener(onAddElementParent)
        parentCollection.removeOnRemoveElementsListener(onRemoveElementParent)
        parentCollection.removeOnReplaceCollectionListener(onReplaceCollectionListener)
    }
}