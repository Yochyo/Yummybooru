package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.components.TagComponent
import de.yochyo.yummybooru.utils.general.addToCopy
import de.yochyo.yummybooru.utils.general.removeFromCopy
import kotlinx.android.synthetic.main.fragment_tag_history.*

class TagHistoryFragmentAdapter(val fragment: TagHistoryFragment) : RecyclerView.Adapter<TagHistoryFragmentViewHolder>() {
    var tags: List<Tag> = emptyList()

    fun update(t: List<Tag>) {
        tags = t
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagHistoryFragmentViewHolder {
        val component = TagComponent(fragment.fragment_tag_history, parent)
        component.onSelect = { tag, selected ->
            if (selected) fragment.viewModel.selectedTagsValue.value.addToCopy(tag.name)
            else fragment.viewModel.selectedTagsValue.value.removeFromCopy(tag.name)
        }
        return TagHistoryFragmentViewHolder(component)
    }

    override fun onBindViewHolder(holder: TagHistoryFragmentViewHolder, position: Int) {
        val tag = tags[position]
        holder.component.update(tag, fragment.viewModel.selectedTagsValue.value.contains(tag.name))
    }

    override fun getItemCount(): Int = tags.size
}