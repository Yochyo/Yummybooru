package de.yochyo.yummybooru.utils.analytics

import android.os.Bundle
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.api.entities.typeAsString

class AnalyticsSearchTagEvent(private val tag: Tag) : IAnalyticsEvent {
    override val name = "search_tag"
    override fun createBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString("tag_name", tag.name)
        bundle.putString("tag_type", tag.typeAsString())
        bundle.putString("tag_count", tag.count.toString())
        bundle.putString("tag_favorite", tag.isFavorite.toString())
        bundle.putString("tag_creation", tag.creation.toGMTString())
        bundle.putString("tag_is_followed", (tag.following != null).toString())
        return bundle
    }
}