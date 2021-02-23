package de.yochyo.yummybooru.api.entities

import androidx.room.*
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
        parentColumns = ["id"],
        childColumns = ["server_id"]
    )],
    indices = [Index(value = ["name", "server_id"], unique = true)]
)
class Tag(
    val name: String,
    val type: TagType,
    isFavorite: Boolean = false,
    @Ignore
    val count: Int = 0,
    following: Following? = null,
    val creation: Date = Date(),
    @ColumnInfo(name = "server_id")
    val serverId: Int = -1,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "tagId") val id: Int = 0
) : IObservableObject<Tag, Int> {
    companion object {
        const val CHANGED_FAVORITE = 1
        const val CHANGED_FOLLOWING = 2
        const val FOLLOWING = 3
    }

    var isFavorite = isFavorite
        set(value) {
            field = value
            trigger(CHANGED_FAVORITE)
        }

    @Embedded
    var following = following
        set(value) {
            var trig = CHANGED_FOLLOWING
            if (field == null && value != null) trig = FOLLOWING
            field = value
            trigger(trig)
        }

    constructor(name: String, type: TagType, isFavorite: Boolean, creation: Date, following: Following?, serverId: Int, id: Int) :
            this(name, type, isFavorite, 0, following, creation, serverId, id)


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