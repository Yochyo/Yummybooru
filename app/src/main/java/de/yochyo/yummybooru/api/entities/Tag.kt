package de.yochyo.yummybooru.api.entities

import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.observable.IObservableObject
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.R
import java.util.*

open class Tag(val name: String, type: Int, isFavorite: Boolean = false, val count: Int = 0, sub: Sub? = null, val creation: Date = Date(), var serverID: Int = -1) : Comparable<Tag>, IObservableObject<Tag, Int> {
    var type = type
        set(value) {
            field = value
            trigger(CHANGED_TYPE)
        }
    var isFavorite = isFavorite
        set(value) {
            field = value
            trigger(CHANGED_FAVORITE)
        }
    var sub = sub
    set(value) {
        field = value
        trigger(CHANGED_SUB)
    }

    companion object {
        const val GENERAL = 0
        const val CHARACTER = 4
        const val COPYPRIGHT = 3
        const val ARTIST = 1
        const val META = 5
        const val UNKNOWN = 99
        const val SPECIAL = 100

        const val CHANGED_TYPE = 0
        const val CHANGED_FAVORITE = 1
        const val CHANGED_SUB = 2

        fun isSpecialTag(name: String): Boolean {
            return name == "*" || name.startsWith("height") || name.startsWith("width") || name.startsWith("order") || name.startsWith("rating") || name.contains(" ")
        }

        fun getCorrectTagType(tagName: String, type: Int): Int {
            return if (type in 0..1 || type in 3..5) type
            else if (isSpecialTag(tagName)) SPECIAL
            else UNKNOWN
        }
    }

    //tag, change
    override val onChange = EventHandler<OnChangeObjectEvent<Tag, Int>>()
    protected fun trigger(change: Int) = onChange.trigger(OnChangeObjectEvent(this, change))

    val color: Int
        get() {
            return when (type) {
                GENERAL -> R.color.blue
                CHARACTER -> R.color.green
                COPYPRIGHT -> R.color.violet
                ARTIST -> R.color.dark_red
                META -> R.color.orange
                else -> R.color.white
            }
        }

    override fun toString(): String = name

    override fun compareTo(other: Tag): Int {
        if (isFavorite && !other.isFavorite)
            return -1
        if (!isFavorite && other.isFavorite)
            return 1
        return name.compareTo(other.name)
    }
}