package de.yochyo.eventmanager.interfaces

import de.yochyo.eventmanager.Event

interface Undoable<E : Event> : Cancelable {
    fun undo(e: Event)
}