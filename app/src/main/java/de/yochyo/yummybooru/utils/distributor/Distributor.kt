package de.yochyo.yummybooru.utils.distributor

import android.content.Context

abstract class Distributor<K, V> {
    private val values = HashMap<K, V>()

    private val pointers = HashMap<K, PointerList>()

    protected abstract fun createIfNotExist(context: Context, key: K): V

    private fun getPointerId(key: K): Int {
        if (pointers[key] == null)
            pointers[key] = PointerList()
        return pointers[key]!!.getPointer()
    }

    fun getPointer(context: Context, key: K): Pointer<V> {
        if (values[key] == null)
            values[key] = createIfNotExist(context, key)
        return Pointer(values[key]!!, getPointerId(key))
    }

    fun releasePointer(key: K, v: Pointer<V>) {
        if (pointers[key]?.releasePointer(v.pointerId) == true) {
            values.remove(key)
            pointers.remove(key)
        }
    }

    fun clear() {
        values.clear()
        pointers.clear()
    }
}