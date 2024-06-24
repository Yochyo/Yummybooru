package de.yochyo.eventmanager

fun interface Listener<T : Event> {
    fun onEvent(e: T)
}