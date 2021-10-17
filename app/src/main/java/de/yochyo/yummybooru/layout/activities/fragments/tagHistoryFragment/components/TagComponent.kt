package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LiveData
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.utils.TagUtil
import de.yochyo.yummybooru.utils.commands.CommandDeleteTag
import de.yochyo.yummybooru.utils.commands.CommandFavoriteTag
import de.yochyo.yummybooru.utils.commands.CommandUpdateTag
import de.yochyo.yummybooru.utils.commands.execute
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.underline
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TagComponent(val server: LiveData<Server>, val viewForSnack: View, container: ViewGroup) {
    private lateinit var tag: Tag
    val toolbar: Toolbar = LayoutInflater.from(container.context).inflate(R.layout.search_item_layout, container, false) as Toolbar
    var onSelect: (tag: Tag, selected: Boolean) -> Unit = { _, _ -> }

    init {
        toolbar.inflateMenu(R.menu.activity_main_search_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.main_search_favorite_tag -> CommandFavoriteTag(tag, !tag.isFavorite).execute(viewForSnack)
                R.id.main_search_follow_tag -> TagUtil.followOrUnfollow(viewForSnack, tag)
                R.id.main_search_delete_tag -> CommandDeleteTag(tag).execute(viewForSnack)
                R.id.main_search_edit_tag -> AddTagDialog {
                    GlobalScope.launch {
                        val server = server.value ?: return@launch
                        val new = server.getTag(it).let { new -> tag.copy(name = new.name, type = new.type) }
                        CommandUpdateTag(tag, new).execute(viewForSnack)
                    }
                }.withTag(tag.name).build(viewForSnack.context)
            }
            true
        }

        val check = toolbar.findViewById<CheckBox>(R.id.search_checkbox)

        fun onClick() = onSelect(tag, check.isChecked)
        toolbar.setOnClickListener {
            check.isChecked = !check.isChecked
            onClick()
        }
        check.setOnClickListener { onClick() }
    }

    fun update(tag: Tag, selected: Boolean) {
        this.tag = tag
        toolbar.findViewById<CheckBox>(R.id.search_checkbox).isChecked = selected
        val textView = toolbar.findViewById<TextView>(R.id.search_textview)
        textView.text = tag.name;textView.setColor(tag.color);textView.underline(tag.isFavorite)
        Menus.initMainSearchTagMenu(viewForSnack.context, toolbar.menu, tag)
    }
}
