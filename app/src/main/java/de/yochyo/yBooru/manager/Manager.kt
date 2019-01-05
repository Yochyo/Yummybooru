package de.yochyo.yBooru.manager

import de.yochyo.yBooru.api.Post
import java.lang.NullPointerException

class Manager {
    companion object {
        private val map = HashMap<Array<out String>, Manager>()//tags, manager
        fun get(tags: Array<String>): Manager {
            val m = map[tags]
            if (m != null) return m
            else throw NullPointerException("Manager was not yet initialized")
        }
        fun getOrInit(tags: Array<out String>): Manager{
            val m = map[tags]
            if (m != null) return m
            else return initialize(tags)
        }

        fun initialize(tags: Array<out String>): Manager {
            val manager = map[tags]
            if (manager == null) {
                val m = Manager()
                map[tags] = m
                return m
            } else
                return manager
        }
    }

    val dataSet = ArrayList<Post?>(200)
    val pages = HashMap<Int, List<Post>>()
    var position = -1
    var currentPage = 1
}