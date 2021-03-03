package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview_with_tag_collections

import android.view.View
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder
import de.yochyo.yummybooru.database.entities.TagCollectionWithTags

class TagHistoryCollectionViewHolder(val layout: View, var tagCollectionWithTags: TagCollectionWithTags) : GroupViewHolder(layout) {
}