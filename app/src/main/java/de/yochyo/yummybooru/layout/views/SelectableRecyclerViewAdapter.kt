package de.yochyo.yummybooru.layout.views

import android.graphics.drawable.ColorDrawable
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.R

abstract class SelectableRecyclerViewAdapter<T : SelectableViewHolder>(private val activity: AppCompatActivity, actionbarLayout: Int) : RecyclerView.Adapter<T>() {
    private var isSelecting = false
    val selected = SelectionArray()

    var actionmode: ActionMode? = null
    protected val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(p0: ActionMode, p1: Menu): Boolean {
            p0.menuInflater.inflate(actionbarLayout, p1)
            return true
        }

        override fun onActionItemClicked(p0: ActionMode, p1: MenuItem) = ActionModeClickEvent(p0, p1).apply { onClickMenuItem.trigger(this) }.isCanceled
        override fun onDestroyActionMode(p0: ActionMode) = unselectAll()
        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?) = false
    }
    val onStartSelection = object : EventHandler<StartSelectingEvent>() {
        override fun trigger(e: StartSelectingEvent) {
            isSelecting = true
            if (actionmode == null)
                actionmode = e.activity.startSupportActionMode(actionModeCallback)
            super.trigger(e)
        }
    }
    val onStopSelection = object : EventHandler<StopSelectingEvent>() {
        override fun trigger(e: StopSelectingEvent) {
            isSelecting = false
            actionmode?.finish()
            actionmode = null
            super.trigger(e)
        }
    }
    val onUpdateSelection = object : EventHandler<UpdateSelectionEvent>() {}
    val onClickMenuItem = object : EventHandler<ActionModeClickEvent>() {}


    abstract val onClickLayout: (holder: T) -> Unit
    val onSelectHolder = { holder: T ->
        val pos = holder.adapterPosition
        if (selected.isSelected(pos)) unselect(pos, holder)
        else select(pos, holder)
    }

    val onClickHolder: (holder: T) -> Unit get() = if (isSelecting) onSelectHolder else onClickLayout


    abstract fun createViewHolder(parent: ViewGroup): T


    open fun setListeners(holder: T){
        holder.layout.setOnClickListener { onClickHolder(holder) }
        holder.layout.setOnLongClickListener { onSelectHolder(holder); true }
    }
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): T {
        val holder = createViewHolder(parent)
        setListeners(holder)
        return holder
    }

    override fun onBindViewHolder(holder: T, position: Int) = updateForeground(holder)
    private fun updateForeground(holder: T) {
        val context = holder.layout.context
        if (selected.isSelected(holder.adapterPosition)) holder.layout.foreground = ColorDrawable(context.resources.getColor(R.color.darker))
        else holder.layout.foreground = ColorDrawable(context.resources.getColor(R.color.transparent))
    }


    fun select(position: Int, holder: T? = null) {
        if (selected.isEmpty()) onStartSelection.trigger(StartSelectingEvent(activity))
        selected.put(position)
        if (holder != null) updateForeground(holder) else notifyItemChanged(position)
        onUpdateSelection.trigger(UpdateSelectionEvent(activity))
    }

    fun unselect(position: Int, holder: T? = null) {
        selected.remove(position)
        if (selected.isEmpty()) onStopSelection.trigger(StopSelectingEvent(activity))
        if (holder != null) updateForeground(holder) else notifyItemChanged(position)
        onUpdateSelection.trigger(UpdateSelectionEvent(activity))
    }

    fun selectAll() {
        if (selected.isEmpty())
            onStartSelection.trigger(StartSelectingEvent(activity))
        if (selected.size < itemCount) {
            for (i in 0 until itemCount)
                selected.put(i)
            onUpdateSelection.trigger(UpdateSelectionEvent(activity))
            notifyDataSetChanged()
        }
    }

    fun unselectAll() {
        if (!selected.isEmpty()) {
            onStopSelection.trigger(StopSelectingEvent(activity))
            selected.clear()
            onUpdateSelection.trigger(UpdateSelectionEvent(activity))
            notifyDataSetChanged()
        }
    }

}
class StopSelectingEvent(val activity: AppCompatActivity) : Event()
class StartSelectingEvent(val activity: AppCompatActivity) : Event()
class UpdateSelectionEvent(val activity: AppCompatActivity) : Event()
class ActionModeClickEvent(val actionmode: ActionMode, val menuItem: MenuItem) : Event()