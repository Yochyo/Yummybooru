package de.yochyo.yummybooru.database.entities

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Junction
import androidx.room.Relation
import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.observable.IObservableObject
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.api.entities.Tag

class TagCollectionWithTags(
    @Embedded var collection: TagCollection,
    @Relation(parentColumn = "collectionId", entityColumn = "tagId", associateBy = Junction(TagCollectionTagCrossRef::class))
    private val _tags: List<Tag>
) : IObservableObject<Collection<Tag>, Collection<Tag>> {
    constructor(name: String, serverId: Int) : this(TagCollection(name, serverId), emptyList())

    @Ignore
    override val handler = EventHandler<OnChangeObjectEvent<Collection<Tag>, Collection<Tag>>>()

    @Ignore
    val tags = EventCollection(_tags as MutableCollection<Tag>)

    init {
        tags.registerOnUpdateListener { handler.trigger(OnChangeObjectEvent(tags, tags)) }
    }

    override fun toString(): String = collection.name
}