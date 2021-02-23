package de.yochyo.yummybooru.database.entities

import androidx.room.*
import de.yochyo.yummybooru.api.entities.Server

@Entity(
    foreignKeys = [
        ForeignKey(onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE, entity = Server::class, parentColumns = ["id"], childColumns = ["serverId"])
    ],
    indices = [Index(value = ["name", "serverId"], unique = true)]
)
class TagCollection(
    val name: String,
    val serverId: Int,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "collectionId")
    var id: Int = 0
) {
    override fun toString(): String = "[$id] $name"
}