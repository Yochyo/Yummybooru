package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.utils.general.addFollowing
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.underline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagHistoryFragmentAdapter(val context: Context, val selectedTags: MutableList<String>) : RecyclerView.Adapter<TagFragmentViewHolder>() {
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
                val pos = adapterPosition
                if (pos !in tags.indices) return

                if (check.isChecked) selectedTags.add(toolbar.findViewById<TextView>(R.id.search_textview).text.toString())
                else selectedTags.remove(tags.elementAt(pos).name)
            }

            toolbar.setOnClickListener {
                check.isChecked = !check.isChecked
                onClick()
            }
            check.setOnClickListener { onClick() }

            toolbar.setOnMenuItemClickListener {
                val pos = adapterPosition
                if (pos !in tags.indices) return@setOnMenuItemClickListener true

                val tag = tags.elementAt(pos)
                when (it.itemId) {
                    R.id.main_search_favorite_tag -> tag.isFavorite = !tag.isFavorite
                    R.id.main_search_follow_tag -> {
                        GlobalScope.launch {
                            if (tag.following == null) tag.addFollowing(context)
                            else tag.following = null
                            withContext(Dispatchers.Main) { notifyItemChanged(pos) }
                        }
                    }
                    R.id.main_search_delete_tag -> {
                        ConfirmDialog { context.db.tags -= tag }
                            .withTitle(context.getString(R.string.delete_tag)).withMessage(context.getString(R.string.delete_tag_with_name, tag.name)).build(context)
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