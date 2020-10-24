package de.yochyo.yummybooru.utils.distributor

import java.util.*

class PointerList {
    private val pointers = LinkedList<Int>()

    /**
     * @return returns next pointer id
     */
    fun getPointer(): Int {
        val i = pointers.lastOrNull() ?: 0
        pointers += i
        return i
    }

    /**
     * @param id id of the pointer
     * @return returns true if no pointer points to the resource
     */
    fun releasePointer(id: Int): Boolean {
        pointers.remove(id)
        return pointers.isEmpty()
    }
}