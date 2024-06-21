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
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableRecyclerViewAdapter
import de.yochyo.yummybooru.layout.selectableRecyclerView.StartSelectingEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.StopSelectingEvent
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandDeleteTag
import de.yochyo.yummybooru.utils.commands.CommandFavoriteTag
import de.yochyo.yummybooru.utils.commands.CommandUpdateFollowingTagData
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.underline

class FollowingTagAdapter(val activity: FollowingActivity, recyclerView: RecyclerView) : SelectableRecyclerViewAdapter<FollowingTagViewHolder>(
    activity, recyclerView, R.menu.following_activity_selection_menu
) {
    val followingObserves = FollowingObservers(activity)

    private var followedTags: List<Tag> = emptyList()

    fun updateFollowing(s: List<Tag>) {
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


    override fun createViewHolder(position: Int, parent: ViewGroup): FollowingTagViewHolder {
        val tag = followedTags[position]
        val holder = FollowingTagViewHolder(activity, activity.layoutInflater.inflate(R.layout.subscription_item_layout, parent, false) as FrameLayout, tag)
        val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.activity_subscription_item_menu)
        toolbar.setOnMenuItemClickListener {
            val pos = holder.adapterPosition
            if (pos !in followedTags.indices) return@setOnMenuItemClickListener true

            val tag = followedTags[holder.adapterPosition]
            when (it.itemId) {
                R.id.following_set_favorite -> Command.execute(activity.binding.followingLayout, CommandFavoriteTag(tag, !tag.isFavorite))
                R.id.unfollow -> Command.execute(activity.binding.followingLayout, CommandUpdateFollowingTagData(tag, null))
                R.id.delete_tag -> Command.execute(activity.binding.followingLayout, CommandDeleteTag(tag))
            }
            true
        }
        return holder
    }

    override fun getItemCount(): Int = followedTags.size

    override fun onBindViewHolder(holder: FollowingTagViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val tag = followedTags[position]
        holder.tag = tag

        val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
        val text1 = toolbar.findViewById<TextView>(android.R.id.text1)
        val text2 = toolbar.findViewById<TextView>(android.R.id.text2)
        text1.text = tag.name
        text1.setColor(tag.color)
        text1.underline(tag.isFavorite)
        Menus.initFollowingMenu(activity, toolbar.menu, tag)
        toolbar.menu.setGroupEnabled(0, selected.isEmpty()) //Cannot access menu when selecting items

        val observer = followingObserves.createObserver(activity, activity.viewModel.server, tag)
        text2.text = activity.getString(R.string.number_of_new_pictures, observer.value)
        holder.registerObserver(observer)
    }
}