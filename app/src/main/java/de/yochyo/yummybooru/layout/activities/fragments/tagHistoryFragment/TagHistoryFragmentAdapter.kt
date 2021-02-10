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
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandDeleteTag
import de.yochyo.yummybooru.utils.commands.CommandFavoriteTag
import de.yochyo.yummybooru.utils.commands.CommandUpdateFollowingTagData
import de.yochyo.yummybooru.utils.general.*
import kotlinx.android.synthetic.main.fragment_tag_history.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagHistoryFragmentAdapter(val fragment: TagHistoryFragment, val selectedTags: MutableList<String>) : RecyclerView.Adapter<TagFragmentViewHolder>() {
    val context = fragment.ctx
    private var tags: Collection<Tag> = emptyList()

    fun update(t: Collection<Tag>) {
        tags = t
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagFragmentViewHolder =
        TagFragmentViewHolder((LayoutInflater.from(context).inflate(R.layout.search_item_layout, parent, false) as Toolbar)).apply {
            toolbar.inflateMenu(R.menu.activity_main_search_menu)
            val check = toolbar.findViewById<CheckBox>(R.id.search_checkbox)

            fun onClick() {
                if (check.isChecked) selectedTags.add(toolbar.findViewById<TextView>(R.id.search_textview).text.toString())
                else selectedTags.remove(tags.elementAt(adapterPosition).name)
            }

            toolbar.setOnClickListener {
                check.isChecked = !check.isChecked
                onClick()
            }
            check.setOnClickListener { onClick() }

            toolbar.setOnMenuItemClickListener {
                GlobalScope.launch(TagDispatcher) {
                    val tag = tags.elementAt(adapterPosition)
                    when (it.itemId) {
                        R.id.main_search_favorite_tag -> Command.execute(fragment.fragment_tag_history, CommandFavoriteTag(tag, !tag.isFavorite))
                        R.id.main_search_follow_tag -> {
                            if (tag.following == null) tag.addFollowing(fragment.fragment_tag_history)
                            else Command.execute(fragment.fragment_tag_history, CommandUpdateFollowingTagData(tag, null))
                            withContext(Dispatchers.Main) { notifyItemChanged(adapterPosition) }
                        }
                        R.id.main_search_delete_tag -> Command.execute(fragment.fragment_tag_history, CommandDeleteTag(tag))
                    }
                }
                true
            }
        }

    override fun onBindViewHolder(holder: TagFragmentViewHolder, position: Int) {
        val tag = tags.elementAt(position)
        holder.toolbar.findViewById<CheckBox>(R.id.search_checkbox).isChecked = selectedTags.contains(tag.name)
        val textView = holder.toolbar.findViewById<TextView>(R.id.search_textview)
        textView.text = tag.name;textView.setColor(tag.color);textView.underline(tag.isFavorite)
        Menus.initMainSearchTagMenu(context, holder.toolbar.menu, tag)
    }

    override fun getItemCount(): Int = tags.size
}