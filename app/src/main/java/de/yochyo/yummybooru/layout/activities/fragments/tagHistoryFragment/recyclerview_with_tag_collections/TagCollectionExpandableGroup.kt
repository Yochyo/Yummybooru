package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview_with_tag_collections

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.entities.TagCollection
import de.yochyo.yummybooru.database.entities.TagCollectionWithTags

class TagCollectionExpandableGroup(
    val collection: TagCollection,
    val tags: List<Tag>
) : ExpandableGroup<Tag>(collection.name, tags) {
    fun toEntity() = TagCollectionWithTags(collection, tags)
}

fun TagCollectionWithTags.toExpandableGroup() = TagCollectionExpandableGroup(collection, tags)