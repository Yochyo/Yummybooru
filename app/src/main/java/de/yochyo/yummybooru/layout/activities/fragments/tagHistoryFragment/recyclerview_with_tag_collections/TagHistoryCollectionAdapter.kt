package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview_with_tag_collections

import android.view.ViewGroup
import android.widget.TextView
import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.entities.TagCollectionWithTags
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.components.TagCollectionComponent
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.components.TagComponent
import de.yochyo.yummybooru.utils.general.addToCopy
import de.yochyo.yummybooru.utils.general.removeFromCopy
import kotlinx.android.synthetic.main.fragment_tag_history.*

class TagHistoryCollectionAdapter(val fragment: TagHistoryCollectionFragment) :
    ExpandableRecyclerViewAdapter<TagHistoryCollectionViewHolder, TagHistoryCollectionChildViewHolder>(listOf(TagCollectionWithTags("Loading...", 0).toExpandableGroup())) {

    fun update(collections: List<TagCollectionExpandableGroup>) {
        val array = if (collections.size < expandableList.groups.size)
            Array(collections.size) { false }
        else
            Array(collections.size) {
                if (expandableList.expandedGroupIndexes.getOrNull(it) == null) false
                else expandableList.expandedGroupIndexes[it]
            }
        expandableList.groups = collections
        expandableList.expandedGroupIndexes = array.toBooleanArray()
        notifyDataSetChanged()
    }

    override fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): TagHistoryCollectionViewHolder {
        val component = TagCollectionComponent(fragment.viewModel, fragment.fragment_tag_history, parent, fragment)
        return TagHistoryCollectionViewHolder(component)
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): TagHistoryCollectionChildViewHolder {
        val component = TagComponent(fragment.fragment_tag_history, parent)
        component.onSelect = { tag, selected ->
            fragment.viewModel.selectedTags.value =
                if (selected) fragment.viewModel.selectedTagsValue.value.addToCopy(tag.name)
                else fragment.viewModel.selectedTagsValue.value.removeFromCopy(tag.name)
        }
        return TagHistoryCollectionChildViewHolder(component)
    }

    override fun onBindChildViewHolder(holder: TagHistoryCollectionChildViewHolder, flatPosition: Int, group: ExpandableGroup<*>, childIndex: Int) {
        val collection = group as TagCollectionExpandableGroup
        val tag = collection.tags[childIndex]
        holder.component.update(tag, fragment.viewModel.selectedTagsValue.value.contains(tag.name))
    }

    override fun onBindGroupViewHolder(holder: TagHistoryCollectionViewHolder, flatPosition: Int, group: ExpandableGroup<*>) {
        holder.component.collection = group as TagCollectionExpandableGroup
        val textView = holder.component.toolbar.findViewById<TextView>(R.id.search_textview)
        textView.text = group.collection.name
    }
}