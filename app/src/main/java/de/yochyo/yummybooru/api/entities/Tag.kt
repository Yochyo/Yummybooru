package de.yochyo.yummybooru.api.entities

import de.yochyo.booruapi.api.TagType
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.observable.IObservableObject
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.R
import java.util.*

open class Tag(
    val name: String,
    type: TagType,
    isFavorite: Boolean = false,
    val count: Int = 0,
    following: Following? = null,
    val creation: Date = Date(),
    var serverID: Int = -1
) :
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
            if (field == null && value != null) trig = FOLLOWING
            field = value
            trigger(trig)
        }

    companion object {
        const val CHANGED_TYPE = 0
        const val CHANGED_FAVORITE = 1
        const val CHANGED_FOLLOWING = 2
        const val FOLLOWING = 3
    }

    //tag, change
    override val onChange = EventHandler<OnChangeObjectEvent<Tag, Int>>()
    protected fun trigger(change: Int) {
        onChange.trigger(OnChangeObjectEvent(this, change))
    }

    val color: Int
        get() {
            return when (type) {
                TagType.GENERAL -> R.color.blue
                TagType.CHARACTER -> R.color.green
                TagType.COPYRIGHT -> R.color.violet
                TagType.ARTIST -> R.color.dark_red
                TagType.META -> R.color.orange
                else -> R.color.cyan
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