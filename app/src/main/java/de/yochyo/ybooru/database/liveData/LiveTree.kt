package de.yochyo.ybooru.database.liveData

import android.arch.lifecycle.MutableLiveData
import java.util.*

class LiveTree<E> : MutableLiveData<TreeSet<E>>() {
    private val tree = TreeSet<E>()

    val isEmpty: Boolean
        get() = tree.isEmpty()

    init {
        value = tree
    }

    fun add(e: E) {
        if (tree.add(e)) value = tree
    }

    fun addAll(e: Collection<E>) {
        tree.addAll(e)
        value = tree
    }

    fun remove(e: E) {
        if (tree.remove(e)) value = tree
    }

    fun indexOf(e: E) = tree.indexOf(e)
    fun find(run: (e: E) -> Boolean) = tree.find(run)

    operator fun plusAssign(e: E) = add(e)
    operator fun plusAssign(e: Collection<E>) = addAll(e)
    operator fun get(position: Int) = tree.elementAt(position)
    operator fun minusAssign(e: E) = remove(e)
}

fun main() {
    val t = LiveTree<String>()
}