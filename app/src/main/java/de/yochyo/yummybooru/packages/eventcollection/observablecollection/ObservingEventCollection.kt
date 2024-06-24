package de.yochyo.eventcollection.observablecollection

import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.observable.IObservableObject
import de.yochyo.eventmanager.EventHandler
import de.yochyo.eventmanager.Listener
import java.io.Closeable

/**
 * @see EventCollection
 * An EventCollection that triggers an OnChangeObjectEvent when an element in it is changed
 * @param T type of elements contained in the collection, must implement IObservableObject
 * @param A type of the arg that is handed over to the OnChangeObjectEvent
 *
 * @property onElementChange triggers an event when an element in the collection is changed (calls an OnChangeObjectEvent)
 */
open class ObservingEventCollection<T : IObservableObject<T, A>, A>(collection: MutableCollection<T>) : EventCollection<T>(collection), IObservableCollection<T, A>, Closeable {
    private val onChangeListener = Listener<OnChangeObjectEvent<T, A>> { onElementChange.trigger(OnChangeObjectEvent(it.new, it.arg)) }
    override val onElementChange = object : EventHandler<OnChangeObjectEvent<T, A>>() {
        override fun trigger(e: OnChangeObjectEvent<T, A>) {
            super.trigger(e)
            notifyChange()
        }
    }

    init {
        collection.forEach { it.registerListener(onChangeListener) }
        onAddElements.registerListener { it.elements.forEach { element -> element.registerListener(onChangeListener) } }

        onRemoveElements.registerListener { it.elements.forEach { element -> element.removeListener(onChangeListener) } }

        onReplaceCollection.registerListener {
            for (element in it.old)
                element.removeListener(onChangeListener)
            for (element in it.new)
                element.registerListener(onChangeListener)
        }
    }

    override fun close() {
        for (element in collection)
            element.removeListener(onChangeListener)
    }


    override fun registerOnElementChangeListener(l: Listener<OnChangeObjectEvent<T, A>>) = onElementChange.registerListener(l)
    override fun removeOnElementChangeListener(l: Listener<OnChangeObjectEvent<T, A>>) = onElementChange.removeListener(l)
    override fun triggerOnElementChangeEvent(e: OnChangeObjectEvent<T, A>) = onElementChange.trigger(e)
}