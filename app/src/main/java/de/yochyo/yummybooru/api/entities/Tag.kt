package de.yochyo.yummybooru.api.entities

import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.observable.IObservableObject
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.R
import java.util.*

open class Tag(val name: String, type: Int, isFavorite: Boolean = false, val count: Int = 0, following: Following? = null, val creation: Date = Date(), var serverID: Int = -1) :
    Comparable<Tag>, IObservableObject<Tag, Int> {
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
    var following = following
    set(value) {
        var trig = CHANGED_FOLLOWING
        if(field == null && value != null) trig = FOLLOWING
        field = value
        trigger(trig)
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
        const val CHANGED_FOLLOWING = 2
        const val FOLLOWING = 3
    }

    //tag, change
    override val onChange = EventHandler<OnChangeObjectEvent<Tag, Int>>()
    protected fun trigger(change: Int){
        onChange.trigger(OnChangeObjectEvent(this, change))
    }

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

    override fun equals(other: Any?) = if (other is Tag) (compareTo(other) == 0) else false
    override fun compareTo(other: Tag): Int {
        if (name == other.name) return 0
        if (isFavorite && !other.isFavorite)
            return -1
        if (!isFavorite && other.isFavorite)
            return 1
        return name.compareTo(other.name)
    }
}

fun Tag.typeAsString(): String {
    return when (type) {
        Tag.GENERAL -> "GENERAL"
        Tag.CHARACTER -> "CHARACTER"
        Tag.COPYPRIGHT -> "COPYPRIGHT"
        Tag.ARTIST -> "ARTIST"
        Tag.META -> "META"
        else -> "UNKNOWN"
    }
}