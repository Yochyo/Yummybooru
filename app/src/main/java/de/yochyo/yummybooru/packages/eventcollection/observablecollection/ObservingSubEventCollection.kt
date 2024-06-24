package de.yochyo.eventcollection.observablecollection

import de.yochyo.eventcollection.IEventCollection
import de.yochyo.eventcollection.SubEventCollection
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.events.OnReplaceCollectionEvent
import de.yochyo.eventcollection.observable.IObservableObject
import de.yochyo.eventmanager.EventHandler
import de.yochyo.eventmanager.Listener

/**
 * @see SubEventCollection
 * A SubEventCollection that triggers an OnChangeObjectEvent when an element in it is changed
 * @param T type of elements contained in the collection, must implement IObservableObject
 * @param A type of the arg that is handed over to the OnChangeObjectEvent
 *
 * @property onElementChange triggers an event when an element in the collection is changed (calls an OnChangeObjectEvent)
 */
open class ObservingSubEventCollection<T : IObservableObject<T, A>, A>(c: MutableCollection<T>, parentCollection: IEventCollection<T>, filter: (e: T) -> Boolean) :
    SubEventCollection<T>(c, parentCollection, filter), IObservableCollection<T, A> {
    private val onChangeElementInParent = Listener<OnChangeObjectEvent<T, A>> {
        if (!contains(it.new)) {
            collection.add(it.new)
            onAddElements.trigger(OnAddElementsEvent(collection, listOf(it.new)))
            notifyChange()
        }
    }

    private val onChangeListener = Listener<OnChangeObjectEvent<T, A>> {
        if (filter(it.new)) onElementChange.trigger(OnChangeObjectEvent(it.new, it.arg))
        else removeFromCollection(listOf(it.new))
    }

    override val onElementChange = object : EventHandler<OnChangeObjectEvent<T, A>>() {
        override fun trigger(e: OnChangeObjectEvent<T, A>) {
            super.trigger(e)
            notifyChange()
        }
    }

    private val onReplaceCollectionListener = Listener<OnReplaceCollectionEvent<T>> {
        for (element in it.old)
            element.removeListener(onChangeListener)
        onReplaceCollection.trigger(OnReplaceCollectionEvent(it.old, it.new))
    }

    init {
        if (parentCollection is IObservableCollection<*, *>) {
            val parent = parentCollection as IObservableCollection<T, A>
            parent.registerOnElementChangeListener(onChangeElementInParent)
        }

        parentCollection.registerOnReplaceCollectionListener(onReplaceCollectionListener)

        collection.forEach { it.registerListener(onChangeListener) }
        onAddElements.registerListener { it.elements.forEach { element -> element.registerListener(onChangeListener) } }
        onRemoveElements.registerListener { it.elements.forEach { element -> element.removeListener(onChangeListener) } }
    }

    override fun close() {
        super.close()
        if (parentCollection is IObservableCollection<*, *>) {
            val parent = parentCollection as IObservableCollection<T, A>
            parent.removeOnElementChangeListener(onChangeElementInParent)
        }
        parentCollection.removeOnReplaceCollectionListener(onReplaceCollectionListener)

        collection.forEach { it.removeListener(onChangeListener) }
    }
}