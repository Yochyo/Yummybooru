package de.yochyo.ybooru.utils

/*object BitmapCache {
    private val bitmaps = LinkedList<Pair<String, Bitmap>>()
    private val MAX_MEMORY = 1024 * 1024 * 512

    var totalSize = 0

    fun storeTemp(id: String, b: Bitmap) {
        bitmaps += Pair(id, b)
        totalSize += b.byteCount
        cleanList()
    }

    fun retrieve(id: String): Bitmap? {
        val indexOf = bitmaps.indexOfFirst { it.first == id }
        if (indexOf != -1) {
            val pair = bitmaps.removeAt(indexOf)
            totalSize -= pair.second.byteCount
            return pair.second
        } else return null
    }

    private fun cleanList() {
        while (totalSize > MAX_MEMORY) {
            if (bitmaps.isNotEmpty()) {
                totalSize -= bitmaps.removeFirst().second.byteCount
            }
        }
    }
}*/