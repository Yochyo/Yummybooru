package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.TagHistoryTagAdapterUtil
import de.yochyo.yummybooru.utils.general.ctx

class TagHistoryFragmentTagAdapter(val fragment: TagHistoryTagFragment) : RecyclerView.Adapter<TagFragmentViewHolder>() {
    val context = fragment.ctx
    val viewModel = fragment.viewModel
    private val util = TagHistoryTagAdapterUtil(fragment, this)

    fun update(t: List<Tag>) {
        util.tags = t
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagFragmentViewHolder = util.onCreateViewHolder(parent, viewType)
    override fun onBindViewHolder(holder: TagFragmentViewHolder, position: Int) = util.onBindViewHolder(holder, position)
    override fun getItemCount(): Int = util.tags.size
}