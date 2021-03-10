package de.yochyo.yummybooru.api.entities

import android.os.Parcel
import android.os.Parcelable
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
) : Parcelable {
    constructor(name: String, type: TagType, serverId: Int, isFavorite: Boolean, creation: Date, following: Following?, id: Int) :
            this(name, type, serverId, isFavorite, 0, following, creation, id)

    constructor(p: Parcel) :
            this(
                p.readString()!!, TagType.valueOf(p.readInt()), p.readInt(), p.readInt() == 1, p.readInt(),
                p.let {
                    val lastId = p.readInt()
                    val lastCount = p.readInt()
                    if (lastId == -1 || lastCount == -1) null
                    else Following(lastId, lastCount)
                }, Date(p.readLong()), p.readInt()
            )

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<Tag> {
            override fun createFromParcel(source: Parcel): Tag = Tag(source)

            override fun newArray(size: Int): Array<Tag?> = Array(size) { null }
        }
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
    override fun describeContents(): Int = 0

    override fun writeToParcel(p: Parcel, flags: Int) {
        p.writeString(name)
        p.writeInt(serverId)
        p.writeInt(if (isFavorite) 1 else 0)
        p.writeInt(count)
        p.writeInt(following?.lastID ?: -1)
        p.writeInt(following?.lastCount ?: -1)
        p.writeLong(creation.time)
        p.writeInt(id)

    }

    override fun equals(other: Any?) = if (other is Tag) name == other.name && serverId == other.serverId else false
}