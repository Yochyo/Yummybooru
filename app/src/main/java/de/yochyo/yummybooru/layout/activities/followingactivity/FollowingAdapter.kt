package de.yochyo.yummybooru.layout.activities.followingactivity

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableRecyclerViewAdapter
import de.yochyo.yummybooru.layout.selectableRecyclerView.StartSelectingEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.StopSelectingEvent
import de.yochyo.yummybooru.utils.commands.*
import de.yochyo.yummybooru.utils.general.TagDispatcher
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.underline
import kotlinx.android.synthetic.main.activity_following.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FollowingTagAdapter(val activity: FollowingActivity, recyclerView: RecyclerView, s: Collection<Tag>) : SelectableRecyclerViewAdapter<FollowingTagViewHolder>(
    activity, recyclerView, R.menu.following_activity_selection_menu
) {
    private val db = activity.db
    val util = FollowingCountUtil(activity)

    private var followedTags: Collection<Tag> = s

    fun updateFollowing(s: Collection<Tag>) {
        followedTags = s
        notifyDataSetChanged()
    }

    private val disableMenuClick = Listener<StartSelectingEvent> { notifyDataSetChanged() }
    private val reEnableMenuClick = Listener<StopSelectingEvent> { notifyDataSetChanged() }

    init {
        onStartSelection.registerListener(disableMenuClick)
        onStopSelection.registerListener(reEnableMenuClick)

        onClickMenuItem.registerListener {
            when (it.menuItem.itemId) {
                R.id.select_all -> if (selected.size == followedTags.size) deselectAll() else selectAll()
                R.id.open_selected -> {
                    Toast.makeText(activity, activity.getString(R.string.not_implemented), Toast.LENGTH_SHORT).show()
                    deselectAll()
                }
                R.id.update_following -> activity.updateFollowing(followedTags, activity.getString(R.string.update_selected_followed_tags))
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

    override fun getItemCount(): Int = followedTags.size
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): FollowingTagViewHolder {
        val holder = super.onCreateViewHolder(parent, position)
        val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.activity_subscription_item_menu)
        toolbar.setOnMenuItemClickListener {
            val pos = holder.adapterPosition
            if (pos !in followedTags.indices) return@setOnMenuItemClickListener true

            GlobalScope.launch(TagDispatcher) {
                val tag = followedTags.elementAt(holder.adapterPosition)
                when (it.itemId) {
                    R.id.following_set_favorite -> Command.execute(activity.following_layout, CommandFavoriteTag(tag, !tag.isFavorite))
                    R.id.unfollow -> Command.execute(activity.following_layout, CommandUpdateFollowingTagData(tag, null))
                    R.id.delete_tag -> Command.execute(activity.following_layout, CommandDeleteTag(tag))
                }
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
        val tag = followedTags.elementAt(position)
        text1.text = tag.name
        text1.setColor(tag.color)
        text1.underline(tag.isFavorite)
        text2.text = activity.getString(R.string.number_of_new_pictures, util.getCount(tag))
        Menus.initFollowingMenu(activity, toolbar.menu, tag)
        toolbar.menu.setGroupEnabled(0, selected.isEmpty()) //Cannot access menu when selecting items
    }
}