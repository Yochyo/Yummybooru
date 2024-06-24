package de.yochyo.eventcollection.observablecollection

import de.yochyo.eventcollection.IEventCollection
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventmanager.EventHandler
import de.yochyo.eventmanager.Listener

interface IObservableCollection<E, A> : IEventCollection<E> {
    val onElementChange: EventHandler<OnChangeObjectEvent<E, A>>

    fun registerOnElementChangeListener(l: Listener<OnChangeObjectEvent<E, A>>) = onElementChange.registerListener(l)
    fun removeOnElementChangeListener(l: Listener<OnChangeObjectEvent<E, A>>) = onElementChange.removeListener(l)
    fun triggerOnElementChangeEvent(e: OnChangeObjectEvent<E, A>) = onElementChange.trigger(e)
}