package de.yochyo.ybooru.manager

import de.yochyo.ybooru.api.Post

class Manager {
    companion object {
        private val map = HashMap<String, Manager>()
        fun get(tags: String): Manager {
            val m = map[tags]
            if (m != null) return m
            else throw NullPointerException("Manager ($tags) was not yet initialized")
        }

        fun getOrInit(tags: String): Manager {
            val m = map[tags]
            if (m != null) return m
            else return initialize(tags)
        }

        fun initialize(tags: String): Manager {
            val manager = map[tags]
            if (manager == null) {
                val m = Manager()
                map[tags] = m
                return m
            } else
                return manager
        }

        fun reset(tags: String) {
            map.remove(tags)
        }

        fun resetAll() {
            map.clear()
        }
    }

    val dataSet = ArrayList<Post>(200)
    val pages = HashMap<Int, List<Post>>()
    var position = -1
    var currentPage = 1
    val currentPost: Post?
        get() = dataSet[position]
}