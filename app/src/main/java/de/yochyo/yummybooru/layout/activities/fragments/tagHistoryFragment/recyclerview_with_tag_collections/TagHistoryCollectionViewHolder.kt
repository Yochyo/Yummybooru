package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview_with_tag_collections

import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder
import de.yochyo.yummybooru.layout.components.tag_history.TagCollectionComponent

class TagHistoryCollectionViewHolder(val component: TagCollectionComponent) : GroupViewHolder(component.toolbar) {
    override fun collapse() {
        component.animateCollapse()
    }

    override fun expand() {
        component.animateExpand()
    }
}