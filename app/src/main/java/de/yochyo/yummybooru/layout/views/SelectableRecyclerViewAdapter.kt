package de.yochyo.yummybooru.layout.views

import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import de.yochyo.yummybooru.R

abstract class SelectableRecyclerViewAdapter<T : SelectableViewHolder> : RecyclerView.Adapter<T>() {
    private var isSelecting = false
    val selected = SelectionArray()

    abstract val onClickLayout: (holder: T) -> Unit
    val onSelectItem = { holder: T ->
        val pos = holder.adapterPosition
        if (selected.isSelected(pos)) unselect(pos, holder)
        else select(pos, holder)
    }

    val onClick: (holder: T) -> Unit
        get() {
            return if (isSelecting) onSelectItem
            else onClickLayout
        }

    abstract fun createViewHolder(parent: ViewGroup): T
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): T {
        val holder = createViewHolder(parent)
        holder.layout.setOnClickListener { onClick(holder) }
        holder.layout.setOnLongClickListener { onSelectItem(holder); true }
        return holder
    }

    override fun onBindViewHolder(holder: T, position: Int) = updateForeground(holder)
    fun updateForeground(holder: T) {
        val context = holder.layout.context
        if (selected.isSelected(holder.adapterPosition)) holder.layout.foreground = ColorDrawable(context.resources.getColor(R.color.darker))
        else holder.layout.foreground = ColorDrawable(context.resources.getColor(R.color.transparent))
    }


    fun select(position: Int, holder: T? = null) {
        if (selected.isEmpty())
            _onStartSelecting()
        selected.put(position)
        if (holder != null) updateForeground(holder) else notifyItemChanged(position)
        onUpdate()
    }

    fun unselect(position: Int, holder: T? = null) {
        selected.remove(position)
        if (selected.isEmpty()) _onStopSelecting()
        if (holder != null) updateForeground(holder) else notifyItemChanged(position)
        onUpdate()
    }

    fun selectAll() {
        if (selected.isEmpty())
            _onStartSelecting()
        if (selected.size < itemCount) {
            for (i in 0 until itemCount)
                selected.put(i)
            onUpdate()
            notifyDataSetChanged()
        }
    }

    fun unselectAll() {
        if (!selected.isEmpty()) {
            _onStopSelecting()
            selected.clear()
            onUpdate()
            notifyDataSetChanged()
        }
    }

    private fun _onStartSelecting() {
        isSelecting = true
        onStartSelecting()
    }

    private fun _onStopSelecting() {
        isSelecting = false
        onStopSelecting()
    }

    abstract fun onStartSelecting()
    abstract fun onStopSelecting()
    abstract fun onUpdate()
}