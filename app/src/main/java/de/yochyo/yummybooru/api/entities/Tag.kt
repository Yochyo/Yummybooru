package de.yochyo.yummybooru.api.entities

import androidx.room.*
import de.yochyo.booruapi.api.TagType
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


data class Tag(
    val name: String,
    val type: TagType,
    @ColumnInfo(name = "server_id")
    val serverId: Int,
    val isFavorite: Boolean = false,
    @Ignore
    val count: Int = 0,
    @Embedded
    val following: Following? = null,
    val creation: Date = Date(),

    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "tagId") val id: Int = 0
) {
    constructor(name: String, type: TagType, serverId: Int, isFavorite: Boolean, creation: Date, following: Following?, id: Int) :
            this(name, type, serverId, isFavorite, 0, following, creation, id)

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
    override fun equals(other: Any?) = if (other is Tag) name == other.name && serverId == other.serverId else false
}