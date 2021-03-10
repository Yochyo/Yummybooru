package de.yochyo.yummybooru.layout.alertdialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.database.entities.TagCollectionWithTags
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.underline
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TagHistoryCollectionEditDialog(val tags: List<Tag>, val collection: TagCollectionWithTags) {
    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Edit collection [${collection.collection.name}]")

        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_edit_tag_collection_layout, null, false)
        val recyclerview = layout.findViewById<RecyclerView>(R.id.dialog_tag_collection_edit_recyclerview)
        val adapter = TagHistoryCollectionEditDialogAdapter(tags, collection.tags.map { it } as MutableList<Tag>)
        recyclerview.adapter = adapter
        recyclerview.layoutManager = LinearLayoutManager(context)

        builder.setPositiveButton(context.getString(R.string.positive_button_name)) { _, _ ->
            GlobalScope.launch {
                try {
                    context.db.removeTagsFromCollection(collection.collection, collection.tags)
                    context.db.addTagsToCollection(collection.collection, adapter.selected)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        builder.setNegativeButton(context.getString(R.string.negative_button_name)) { _, _ -> }
        builder.setView(layout)
        builder.show()
    }
}

private class TagHistoryCollectionEditDialogAdapter(val tags: List<Tag>, val selected: MutableList<Tag>) : RecyclerView.Adapter<TagHistoryCollectionEditDialogViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagHistoryCollectionEditDialogViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.dialog_edit_tag_collection_item_layout, parent, false) as Toolbar
        val holder = TagHistoryCollectionEditDialogViewHolder(layout)
        val check = layout.findViewById<CheckBox>(R.id.tag_selected)
        fun onClick() {
            if (check.isChecked) selected.add(tags[holder.adapterPosition])
            else selected.remove(tags[holder.adapterPosition])
        }
        layout.setOnClickListener {
            check.isChecked = !check.isChecked
            onClick()
        }
        check.setOnClickListener { onClick() }
        return holder
    }

    override fun onBindViewHolder(holder: TagHistoryCollectionEditDialogViewHolder, position: Int) {
        val check = holder.toolbar.findViewById<CheckBox>(R.id.tag_selected)
        val textview = holder.toolbar.findViewById<TextView>(R.id.tag_name_textview)

        val tag = tags[position]
        check.isChecked = selected.contains(tag)
        textview.text = tag.name;textview.setColor(tag.color);textview.underline(tag.isFavorite)
    }

    override fun getItemCount(): Int = tags.size

}

private class TagHistoryCollectionEditDialogViewHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)