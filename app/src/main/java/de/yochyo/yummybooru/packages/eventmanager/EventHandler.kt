package de.yochyo.eventmanager

import java.util.LinkedList

open class EventHandler<E : Event> {
    private val lock = Any()

    private var listenersChanged = false
    private val listeners = object : LinkedList<Listener<E>>() {
        override fun add(element: Listener<E>): Boolean {
            listenersChanged = true
            return super.add(element)
        }

        override fun clear() {
            listenersChanged = true
            super.clear()
        }

        override fun remove(): Listener<E> {
            listenersChanged = true
            return super.remove()
        }
    }
    private var copyOfListeners: List<Listener<E>> = LinkedList<Listener<E>>()

    /**
     * Adds a listener to the evenhandler.
     * If an event is being triggered right now, the listener will be added
     * after the event was proceeded.
     */
    open fun registerListener(l: Listener<E>): Listener<E> {
        synchronized(lock) {
            listeners.add(l)
        }
        return l
    }

    /**
     * Removes a listener from the evenhandler.
     * If an event is being triggered right now, the listener will be removed
     * after the event was proceeded.
     */
    open fun removeListener(l: Listener<E>) {
        synchronized(lock) { listeners.remove(l) }
    }

    /**
     * Removes all listeners from the evenhandler. This method has to be used if the listeners should be removed while an event is being triggered.
     * This can happen if a listener calls this function. If an event is being triggered right now, the listeners will be removed
     * after the event was proceeded.
     * Do not use removeAll() if you want to remove all listeners while an event is being triggered
     */
    open fun removeAllListeners() {
        synchronized(lock) { listeners.clear() }
    }


    open fun trigger(e: E) {
        synchronized(lock) {
            if (listenersChanged) copyOfListeners = listeners.map { it }
            val iter = copyOfListeners.iterator()
            while (iter.hasNext()) {
                val next = iter.next()
                next.onEvent(e)
                if (e.deleteListener) {
                    e.deleteListener = false
                    removeListener(next)
                }
            }
        }
    }
}