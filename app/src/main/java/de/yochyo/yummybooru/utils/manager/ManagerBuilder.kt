package de.yochyo.yummybooru.utils.manager

object ManagerBuilder {
    fun toManager(tagString: String): IManager {
        val split = tagString.split(" OR ")
        val managers = ArrayList<IManager>()
        for (s in split) managers += NewManager(s)
        if(managers.size == 1) return managers.first()
        println(managers)
        return NewManagerFolder(managers)
    }

    fun toManagerFolder(managers: Collection<IManager>) = NewManagerFolder(managers)
}