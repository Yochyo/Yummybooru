package de.yochyo.yummybooru.layout.selectableRecyclerView

import android.graphics.drawable.ColorDrawable
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.dragselectrecyclerview.DragSelectReceiver
import com.afollestad.dragselectrecyclerview.DragSelectTouchListener
import com.afollestad.dragselectrecyclerview.Mode
import de.yochyo.eventmanager.Event
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.R

abstract class SelectableRecyclerViewAdapter<T : SelectableViewHolder>(private val activity: AppCompatActivity, actionbarLayout: Int) : RecyclerView.Adapter<T>() {
    private var isSelecting = false
    val selected = SelectionArray()

    //tells receiver how the selection was triggered
    private var startedLongClick: Boolean? = false
    private val SELECT_BY_LONG_CLICK = true
    private val SELECT_BY_DRAG = null
    private val SELECT_BY_SHORT_CLICK = false


    private val receiver = object : DragSelectReceiver {
        //tells setSelected() if we're in selection- or deselection-mode
        private var isDragSelecting = false
        private val SELECTING_MODE = true
        private val UNSELECTING_MODE = false

        override fun getItemCount() = itemCount

        override fun isIndexSelectable(index: Int) = true

        override fun isSelected(index: Int) = selected.isSelected(index)

        override fun setSelected(index: Int, selected: Boolean) {
            val current = isSelected(index)
            if(startedLongClick == SELECT_BY_LONG_CLICK){
                startedLongClick = SELECT_BY_DRAG
                isDragSelecting = !current
            }
            if(startedLongClick == SELECT_BY_DRAG){
                if (!current && isDragSelecting == SELECTING_MODE) select(index)
                else if (current && isDragSelecting == UNSELECTING_MODE) deselect(index)
            }
            else {
                if (current) deselect(index)
                else select(index)
            }
        }
    }
    val dragListener = DragSelectTouchListener.create(activity, receiver) {
        mode = Mode.PATH
    }


    fun isDragSelectingEnabled(recyclerView: RecyclerView, enabled: Boolean){
        if(enabled) recyclerView.addOnItemTouchListener(dragListener)
        else recyclerView.removeOnItemTouchListener(dragListener)
    }

    var actionmode: ActionMode? = null
    protected val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(p0: ActionMode, p1: Menu): Boolean {
            p0.menuInflater.inflate(actionbarLayout, p1)
            return true
        }

        override fun onActionItemClicked(p0: ActionMode, p1: MenuItem) = true.apply { onClickMenuItem.trigger(ActionModeClickEvent(p0, p1)) }
        override fun onDestroyActionMode(p0: ActionMode) = deselectAll()
        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?) = false
    }

    val onStartSelection = object : EventHandler<StartSelectingEvent>() {
        override fun trigger(e: StartSelectingEvent) {
            isSelecting = true
            if (actionmode == null) {
                actionmode = e.activity.startSupportActionMode(actionModeCallback)
                onUpdateSelection.trigger(UpdateSelectionEvent(activity))
            }
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
    val onUpdateSelection = object : EventHandler<UpdateSelectionEvent>() {
        override fun trigger(e: UpdateSelectionEvent) {
            actionmode?.title = "${selected.size}/$itemCount"
            super.trigger(e)
        }
    }
    val onClickMenuItem = object : EventHandler<ActionModeClickEvent>() {}


    val onLongClickViewHolder = { holder: T ->
        val pos = holder.adapterPosition
        startedLongClick = SELECT_BY_LONG_CLICK
        dragListener.setIsActive(true, pos)
    }
    val onClickViewHolder = { holder: T ->
        startedLongClick = SELECT_BY_SHORT_CLICK
        receiver.setSelected(holder.adapterPosition, !selected.isSelected(holder.adapterPosition))
    }

    fun clickHolder(holder: T) {
        if (isSelecting) onClickViewHolder(holder)
        else holder.onClickLayout()
    }

    abstract fun createViewHolder(parent: ViewGroup): T


    open fun setListeners(holder: T) {
        holder.layout.setOnClickListener { clickHolder(holder) }
        holder.layout.setOnLongClickListener { onLongClickViewHolder(holder); true }
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


    fun select(position: Int) {
        if (selected.isEmpty()) onStartSelection.trigger(StartSelectingEvent(activity))
        selected.put(position)
        notifyItemChanged(position)
        onUpdateSelection.trigger(UpdateSelectionEvent(activity))
    }

    fun deselect(position: Int) {
        selected.remove(position)
        if (selected.isEmpty()) onStopSelection.trigger(StopSelectingEvent(activity))
        notifyItemChanged(position)
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

    fun deselectAll() {
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