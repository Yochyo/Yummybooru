package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview_with_tag_collections

import android.view.View
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder
import de.yochyo.yummybooru.api.entities.Tag

class TagHistoryCollectionChildViewHolder(val layout: View, var tag: Tag) : ChildViewHolder(layout) {
}