package de.yochyo.yummybooru.layout.activities.followingactivity

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableRecyclerViewAdapter
import de.yochyo.yummybooru.layout.selectableRecyclerView.StartSelectingEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.StopSelectingEvent
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.underline
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FollowingTagAdapter(val activity: FollowingActivity, s: Collection<Tag>) : SelectableRecyclerViewAdapter<FollowingTagViewHolder>(activity, R.menu.following_activity_selection_menu) {
    private val db = activity.db
    val util = FollowingCountUtil(activity)

    private var following: Collection<Tag> = s

    fun updateFollowing(s: Collection<Tag>) {
        following = s
        notifyDataSetChanged()
    }

    private val disableMenuClick = Listener.create<StartSelectingEvent> { notifyDataSetChanged() }
    private val reEnableMenuClick = Listener.create<StopSelectingEvent> { notifyDataSetChanged() }

    init {
        onStartSelection.registerListener(disableMenuClick)
        onStopSelection.registerListener(reEnableMenuClick)

        onClickMenuItem.registerListener {
            when (it.menuItem.itemId) {
                R.id.select_all -> if (selected.size == following.size) unselectAll() else selectAll()
                R.id.open_selected -> {
                    Toast.makeText(activity, activity.getString(R.string.not_implemented), Toast.LENGTH_SHORT).show()
                    unselectAll()
                }
                R.id.update_following -> {
                    ConfirmDialog {
                        val select = selected.getSelected(following)
                        unselectAll()
                        GlobalScope.launch { activity.updateFollowing(select) }
                    }.withTitle(activity.getString(R.string.update_selected_followed_tags)).build(activity)
                }
            }
        }
    }

    override fun setListeners(holder: FollowingTagViewHolder) {
        val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setOnClickListener { clickHolder(holder) }
        toolbar.setOnLongClickListener { onLongClickViewHolder(holder); true }
    }


    override fun createViewHolder(parent: ViewGroup): FollowingTagViewHolder {
        return FollowingTagViewHolder(activity, activity.layoutInflater.inflate(R.layout.subscription_item_layout, parent, false) as FrameLayout)
    }

    private fun deleteSubDialog(tag: Tag) {
        val b = AlertDialog.Builder(activity)
        b.setTitle(R.string.delete)
        b.setMessage(activity.getString(R.string.unfollow_tag_with_name, tag.name))
        b.setNegativeButton(R.string.negative_button_name) { _, _ -> }
        b.setPositiveButton(R.string.positive_button_name) { _, _ -> GlobalScope.launch { tag.following = null } }
        b.show()
    }
    private fun deleteTagDialog(tag: Tag) {
        val b = AlertDialog.Builder(activity)
        b.setTitle(R.string.delete)
        b.setMessage(activity.getString(R.string.delete_tag_with_name, tag.name))
        b.setNegativeButton(R.string.negative_button_name) { _, _ -> }
        b.setPositiveButton(R.string.positive_button_name) { _, _ -> GlobalScope.launch { db.tags -= tag } }
        b.show()
    }

    override fun getItemCount(): Int = following.size
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): FollowingTagViewHolder {
        val holder = super.onCreateViewHolder(parent, position)
        val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.activity_subscription_item_menu)
        toolbar.setOnMenuItemClickListener {
            val sub = following.elementAt(holder.adapterPosition)
            when (it.itemId) {
                R.id.following_set_favorite -> sub.isFavorite = !sub.isFavorite
                R.id.unfollow -> deleteSubDialog(sub)
                R.id.delete_tag -> deleteTagDialog(sub)
            }
            true
        }
        return holder
    }

    override fun onBindViewHolder(holder: FollowingTagViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
        val text1 = toolbar.findViewById<TextView>(android.R.id.text1)
        val text2 = toolbar.findViewById<TextView>(android.R.id.text2)
        val sub = following.elementAt(position)
        text1.text = sub.name
        text1.setColor(sub.color)
        text1.underline(sub.isFavorite)
        text2.text = activity.getString(R.string.number_of_new_pictures, util.getCount(sub))
        Menus.initFollowingMenu(activity, toolbar.menu, sub)
        toolbar.menu.setGroupEnabled(0, selected.isEmpty()) //Cannot access menu when selecting items
    }
}