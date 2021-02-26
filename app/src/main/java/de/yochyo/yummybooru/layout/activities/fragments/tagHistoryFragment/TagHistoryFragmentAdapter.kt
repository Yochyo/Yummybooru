package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.utils.TagUtil
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandDeleteTag
import de.yochyo.yummybooru.utils.commands.CommandFavoriteTag
import de.yochyo.yummybooru.utils.general.*
import kotlinx.android.synthetic.main.fragment_tag_history.*

class TagHistoryFragmentAdapter(val fragment: TagHistoryFragment) : RecyclerView.Adapter<TagFragmentViewHolder>() {
    val context = fragment.ctx
    val viewModel = fragment.viewModel
    private var tags: List<Tag> = emptyList()

    fun update(t: List<Tag>) {
        tags = t
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagFragmentViewHolder =
        TagFragmentViewHolder((LayoutInflater.from(context).inflate(R.layout.search_item_layout, parent, false) as Toolbar)).apply {
            toolbar.inflateMenu(R.menu.activity_main_search_menu)
            val check = toolbar.findViewById<CheckBox>(R.id.search_checkbox)

            fun onClick() {
                viewModel.selectedTags.value =
                    if (check.isChecked) viewModel.selectedTagsValue.value.addToCopy(toolbar.findViewById<TextView>(R.id.search_textview).text.toString())
                    else fragment.viewModel.selectedTagsValue.value.removeFromCopy(toolbar.findViewById<TextView>(R.id.search_textview).text.toString())
            }

            toolbar.setOnClickListener {
                check.isChecked = !check.isChecked
                onClick()
            }
            check.setOnClickListener { onClick() }

            toolbar.setOnMenuItemClickListener {
                val tag = tags[adapterPosition]
                when (it.itemId) {
                    R.id.main_search_favorite_tag -> Command.execute(fragment.fragment_tag_history, CommandFavoriteTag(tag, !tag.isFavorite))
                    R.id.main_search_follow_tag -> TagUtil.followOrUnfollow(fragment.fragment_tag_history, tag)
                    R.id.main_search_delete_tag -> Command.execute(fragment.fragment_tag_history, CommandDeleteTag(tag))
                }
                true
            }
        }

    override fun onBindViewHolder(holder: TagFragmentViewHolder, position: Int) {
        val tag = tags.elementAt(position)
        holder.toolbar.findViewById<CheckBox>(R.id.search_checkbox).isChecked = fragment.viewModel.selectedTagsValue.value.contains(tag.name)
        val textView = holder.toolbar.findViewById<TextView>(R.id.search_textview)
        textView.text = tag.name;textView.setColor(tag.color);textView.underline(tag.isFavorite)
        Menus.initMainSearchTagMenu(context, holder.toolbar.menu, tag)
    }

    override fun getItemCount(): Int = tags.size
}