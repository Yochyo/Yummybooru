package de.yochyo.yummybooru.layout.activities.mainactivity

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
import de.yochyo.yummybooru.utils.general.addSub
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.underline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagAdapter(val context: Context, t: Collection<Tag>) : RecyclerView.Adapter<TagViewHolder>() {
    private var tags: Collection<Tag> = t

    fun update(t: Collection<Tag>){
        tags = t
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder = TagViewHolder((LayoutInflater.from(context).inflate(R.layout.search_item_layout, parent, false) as Toolbar)).apply {
        toolbar.inflateMenu(R.menu.activity_main_search_menu)
        val check = toolbar.findViewById<CheckBox>(R.id.search_checkbox)

        fun onClick() {
            if (check.isChecked) MainActivity.selectedTags.add(toolbar.findViewById<TextView>(R.id.search_textview).text.toString())
            else MainActivity.selectedTags.remove(toolbar.findViewById<TextView>(R.id.search_textview).text)
        }
        toolbar.setOnClickListener {
            check.isChecked = !check.isChecked
            onClick()
        }
        check.setOnClickListener {
            onClick()
        }

        toolbar.setOnMenuItemClickListener {
            val tag = tags.elementAt(adapterPosition)
            when (it.itemId) {
                R.id.main_search_favorite_tag -> tag.isFavorite = !tag.isFavorite
                R.id.main_search_subscribe_tag -> {
                    GlobalScope.launch {
                        if(tag.sub == null) tag.addSub(context)
                        else tag.sub = null
                        withContext(Dispatchers.Main) { notifyItemChanged(adapterPosition) }
                    }
                }
                R.id.main_search_delete_tag -> {
                    ConfirmDialog {
                        MainActivity.selectedTags.remove(tag.name)
                        context.db.tags -= tag
                    }.withTitle("Delete").withMessage("Delete tag ${tag.name}").build(context)
                }
            }
            true
        }
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags.elementAt(position)
        holder.toolbar.findViewById<CheckBox>(R.id.search_checkbox).isChecked = MainActivity.selectedTags.contains(tag.name)
        val textView = holder.toolbar.findViewById<TextView>(R.id.search_textview)
        textView.text = tag.name;textView.setColor(tag.color);textView.underline(tag.isFavorite)
        Menus.initMainSearchTagMenu(context, holder.toolbar.menu, tag)
    }

    override fun getItemCount(): Int = tags.size
}