package de.yochyo.yummybooru.layout.selectableRecyclerView

class SelectionArray {
    private val selected = HashMap<Int, Boolean>()
    val size get() = selected.keys.size
    fun isEmpty() = size == 0
    fun put(i: Int) = selected.put(i, true)
    fun remove(i: Int) = selected.remove(i)
    fun isSelected(position: Int) = selected[position] == true
    fun clear() = selected.clear()
    fun <E> getSelected(c: Collection<E>): ArrayList<E> {
        val array = arrayListOf<E>()
        for (i in c.indices) {
            val value = isSelected(i)
            if (value) array += c.elementAt(i)
        }
        return array
    }
}
