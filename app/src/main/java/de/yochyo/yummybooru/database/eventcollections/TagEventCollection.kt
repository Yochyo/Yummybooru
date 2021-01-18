package de.yochyo.yummybooru.database.eventcollections

import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.utils.general.SortedTreeWithPrimaryKey

class TagEventCollection(comparator: Comparator<Tag>) : SortedTreeWithPrimaryKey<Tag, String>(comparator, { it.name })