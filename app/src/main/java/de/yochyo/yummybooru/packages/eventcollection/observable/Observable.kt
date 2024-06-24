package de.yochyo.eventcollection.observable

import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventmanager.EventHandler

/**
 * An implementation of IObservableObject. This class is a wrapper class for simple objects.
 * triggers onChange when the value setter is called.
 * @param t the object that shall be observed
 * @param T type of t
 */
class Observable<T>(t: T) : IObservableObject<Observable<T>, T> {
    var value = t
        set(value) {
            val oldValue = field
            field = value
            handler.trigger(OnChangeObjectEvent(this, oldValue))
        }


    override val handler = EventHandler<OnChangeObjectEvent<Observable<T>, T>>()
    override fun toString() = value.toString()
}