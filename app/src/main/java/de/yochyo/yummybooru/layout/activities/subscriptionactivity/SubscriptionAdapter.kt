package de.yochyo.yummybooru.layout.activities.subscriptionactivity

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableRecyclerViewAdapter
import de.yochyo.yummybooru.layout.selectableRecyclerView.StartSelectingEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.StopSelectingEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.UpdateSelectionEvent
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.underline
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SubscribedTagAdapter(val activity: SubscriptionActivity) : SelectableRecyclerViewAdapter<SubscribedTagViewHolder>(activity, R.menu.subscription_activity_selection_menu) {
    private val db = activity.db
    val util = SubscriptionCountUtil(activity)

    private val startSelectionListener = Listener.create<StartSelectingEvent> {
        actionmode?.title = "${selected.size}/${activity.currentFilter.size}"
    }
    private val updateSelectionListener = Listener.create<UpdateSelectionEvent> {
        actionmode?.title = "${selected.size}/${activity.currentFilter.size}"
    }
    private val disableMenuClick = Listener.create<StartSelectingEvent> { notifyDataSetChanged() }
    private val reEnableMenuClick = Listener.create<StopSelectingEvent> { notifyDataSetChanged() }

    init {
        onStartSelection.registerListener(startSelectionListener)
        onUpdateSelection.registerListener(updateSelectionListener)
        onStartSelection.registerListener(disableMenuClick)
        onStopSelection.registerListener(reEnableMenuClick)

        onClickMenuItem.registerListener {
            when (it.menuItem.itemId) {
                R.id.select_all -> if (selected.size == activity.currentFilter.size) unselectAll() else selectAll()
                R.id.open_selected -> {
                    Toast.makeText(activity, "Not yet implemented", Toast.LENGTH_SHORT).show()
                    unselectAll()
                }
                R.id.update_subs -> {
                    ConfirmDialog {
                        GlobalScope.launch {
                            val id = Api.newestID(activity)
                            for (selected in selected.getSelected(activity.currentFilter)) {
                                val tag = Api.getTag(activity, selected.name)
                                db.changeSubscription(selected.copy(lastCount = tag.count, lastID = id))
                            }
                        }
                        unselectAll()
                    }.withTitle("Update selected subs?").build(activity)
                }
            }
        }
    }

    override fun setListeners(holder: SubscribedTagViewHolder) {
        val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setOnClickListener { clickHolder(holder) }
        toolbar.setOnLongClickListener { onSelectHolder(holder); true }
    }


    override fun createViewHolder(parent: ViewGroup): SubscribedTagViewHolder {
        return SubscribedTagViewHolder(activity, activity.layoutInflater.inflate(R.layout.subscription_item_layout, parent, false) as FrameLayout)
    }

    private fun deleteSubDialog(sub: Subscription) {
        val b = AlertDialog.Builder(activity)
        b.setTitle(R.string.delete)
        b.setMessage("${activity.getString(R.string.delete)} ${activity.getString(R.string.subscription)} ${sub.name}?")
        b.setNegativeButton(R.string.no) { _, _ -> }
        b.setPositiveButton(R.string.yes) { _, _ -> GlobalScope.launch { db.deleteSubscription(sub.name) } }
        b.show()
    }

    override fun getItemCount(): Int = activity.currentFilter.size
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): SubscribedTagViewHolder {
        val holder = super.onCreateViewHolder(parent, position)
        val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.activity_subscription_item_menu)
        toolbar.setOnMenuItemClickListener {
            val sub = activity.currentFilter.elementAt(holder.adapterPosition)
            when (it.itemId) {
                R.id.subscription_set_favorite -> GlobalScope.launch {
                    val copy = sub.copy(isFavorite = !sub.isFavorite)
                    db.changeSubscription(copy)
                }
                R.id.subscription_delete -> deleteSubDialog(sub)
            }
            true
        }
        return holder
    }

    override fun onBindViewHolder(holder: SubscribedTagViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
        val text1 = toolbar.findViewById<TextView>(android.R.id.text1)
        val text2 = toolbar.findViewById<TextView>(android.R.id.text2)
        val sub = activity.currentFilter.elementAt(position)
        text1.text = sub.name
        text1.setColor(sub.color)
        text1.underline(sub.isFavorite)
        text2.text = "${activity.getString(R.string.number_of_new_pictures)} ${util.getCount(sub)}"
        Menus.initSubscriptionMenu(toolbar.menu, sub)
        toolbar.menu.setGroupEnabled(0, selected.isEmpty()) //Cannot access menu when selecting items
    }
}