package de.yochyo.eventmanager

abstract class Event {
    internal var deleteListener = false

    open val name: String
        get() = this::class.java.simpleName!!

    fun removeListener() {
        deleteListener = true
    }
}