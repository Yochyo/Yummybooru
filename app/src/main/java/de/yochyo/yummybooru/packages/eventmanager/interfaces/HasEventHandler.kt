package de.yochyo.eventmanager.interfaces

import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.eventmanager.Listener

interface HasEventHandler<E : Event> {
    val handler: EventHandler<E>

    fun registerListener(l: Listener<E>) = handler.registerListener(l)
    fun removeListener(l: Listener<E>) = handler.removeListener(l)
    fun removeAllListeners() = handler.removeAllListeners()
    fun triggerEvent(e: E) = handler.trigger(e)
}