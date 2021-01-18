package de.yochyo.yummybooru.utils.general

import java.util.*
import kotlin.collections.HashMap

open class SortedTreeWithPrimaryKey<V, K>(comparator: Comparator<V>, private val getPKey: (e: V) -> K) : MutableList<V> {
    private val keys = HashMap<K, V>()
    private val values = TreeSet(comparator)

    override val size: Int get() = values.size
    override fun contains(element: V): Boolean {
        return keys.contains(element.toKey())
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        for (element in elements)
            if (!keys.contains(element.toKey())) return false
        return true
    }

    override fun get(index: Int): V {
        return values.elementAt(index)
    }

    override fun indexOf(element: V): Int {
        val fromKey = keys[element.toKey()] ?: return -1
        return values.indexOf(fromKey)
    }

    override fun isEmpty(): Boolean {
        return keys.isEmpty()
    }

    override fun iterator(): MutableIterator<V> {
        return values.iterator()
    }

    override fun lastIndexOf(element: V): Int {
        val fromKey = keys[element.toKey()] ?: return -1
        return values.lastIndexOf(fromKey)
    }

    override fun add(element: V): Boolean {
        val fromKey = keys[element.toKey()]
        if (fromKey != null) return false
        val res = values.add(element)
        if (res) keys[element.toKey()] = element
        return res
    }

    override fun add(index: Int, element: V) {
        TODO("Not yet implemented")
    }

    override fun addAll(index: Int, elements: Collection<V>): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<V>): Boolean {
        var result = true
        for (element in elements)
            if (!add(element)) result = false
        return result
    }

    override fun clear() {
        keys.clear()
        values.clear()
    }

    override fun listIterator(): MutableListIterator<V> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<V> {
        TODO("Not yet implemented")
    }

    override fun remove(element: V): Boolean {
        val fromKey = keys[element.toKey()] ?: return false
        val res = values.remove(fromKey)
        if (res) keys.remove(element.toKey())
        return res
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        val fromKey = elements.mapNotNull { keys[it.toKey()] }
        var res = true
        for (value in fromKey)
            if (!values.remove(value)) res = false
            else keys.remove(value.toKey())
        return res
    }

    override fun removeAt(index: Int): V {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: V): V {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<V> {
        TODO("Not yet implemented")
    }

    private fun V.toKey() = getPKey(this)
}