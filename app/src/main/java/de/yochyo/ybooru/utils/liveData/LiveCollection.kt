package de.yochyo.ybooru.utils.liveData

import android.arch.lifecycle.LiveData
import java.util.*
import kotlin.collections.ArrayList


class LiveArrayList<T> : LiveCollection<ArrayList<T>, T>(ArrayList())
class LiveTree<E> : LiveCollection<TreeSet<E>, E>(TreeSet())
abstract class LiveCollection<C : MutableCollection<T>, T>(collection: C) : LiveData<C>() {
    private val v: C = collection

    init {
        notifyChange()
    }

    val isEmpty: Boolean get() = v.isEmpty()

    fun add(e: T) {
        if (v.add(e)) notifyChange()
    }

    fun addAll(e: Collection<T>) {
        if (v.addAll(e)) notifyChange()
    }

    fun remove(e: T) {
        if (v.remove(e)) notifyChange()
    }

    fun clear() {
        v.clear()
        notifyChange()
    }

    fun find(run: (e: T) -> Boolean) = v.find(run)
    val size: Int get() = v.size
    operator fun plusAssign(e: T) = add(e)
    operator fun plusAssign(e: Collection<T>) = addAll(e)
    operator fun get(position: Int) = v.elementAt(position)
    operator fun minusAssign(e: T) = remove(e)

    fun notifyChange() {
        value = v
    }
}