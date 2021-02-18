package de.yochyo.yummybooru.api.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import de.yochyo.booruapi.api.TagType
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.observable.IObservableObject
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.R
import java.util.*

@Entity(
    tableName = "tags",
    foreignKeys = [ForeignKey(
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE,
        entity = Server::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("server_id")
    )],
    primaryKeys = ["name", "server_id"]
)
class Tag : IObservableObject<Tag, Int> {
    companion object {
        const val CHANGED_FAVORITE = 1
        const val CHANGED_FOLLOWING = 2
        const val FOLLOWING = 3
    }

    val name: String
    val type: TagType

    @ColumnInfo(name = "isFavorite")
    private var _isFavorite: Boolean
    var isFavorite
        get() = _isFavorite
        set(value) {
            _isFavorite = value
            trigger(CHANGED_FAVORITE)
        }

    @Ignore
    val count: Int

    @ColumnInfo(name = "last_count")
    var lastCount: Int?
        private set

    @ColumnInfo(name = "last_id")
    var lastId: Int?
        private set

    val creation: Date

    @ColumnInfo(name = "server_id")
    val serverId: Int

    constructor(
        name: String,
        type: TagType,
        isFavorite: Boolean = false,
        count: Int = 0,
        lastCount: Int? = null,
        lastId: Int? = null,
        creation: Date = Date(),
        serverId: Int = -1,
    ) {
        this.name = name
        this.type = type
        this._isFavorite = isFavorite
        this.count = count
        this.lastCount = lastCount
        this.lastId = lastId
        this.creation = creation
        this.serverId = serverId
    }

    constructor(name: String, type: TagType, isFavorite: Boolean, creation: Date, lastCount: Int?, lastId: Int?, serverId: Int) :
            this(name, type, isFavorite, 0, lastCount, lastId, creation, serverId)


    fun setFollowing(lastCount: Int?, lastId: Int?) {
        if (this.lastCount != lastCount && this.lastId != lastId) {
            val trig = if (this.lastCount == null && this.lastId == null && lastCount != null && lastId != null) FOLLOWING else CHANGED_FOLLOWING
            this.lastCount = lastCount
            this.lastId = lastId
            trigger(trig)
        }
    }

    @Ignore
    override val onChange = EventHandler<OnChangeObjectEvent<Tag, Int>>()
    private fun trigger(change: Int) {
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
    override fun equals(other: Any?) = if (other is Tag) name == other.name else false
}