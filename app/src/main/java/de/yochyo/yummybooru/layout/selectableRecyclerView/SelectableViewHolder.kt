package de.yochyo.yummybooru.layout.selectableRecyclerView

import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView

abstract class SelectableViewHolder(val layout: FrameLayout) : RecyclerView.ViewHolder(layout){
    abstract fun onClickLayout()
}