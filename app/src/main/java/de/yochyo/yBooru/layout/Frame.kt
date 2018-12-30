package de.yochyo.yBooru.layout

import android.content.Context
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout

class Frame(context: Context): LinearLayout(context){
    init{
        orientation = LinearLayout.VERTICAL
        this.addView(bar)
        this.addView(bar)
    }

    val bar: LinearLayout
    get(){
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.HORIZONTAL
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 2f)
        val b1 = Button(context).also { it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT, 1f) }.also { it.text = "1"}
        val b2 = Button(context).also { it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT, 1f) }.also{it.text = "2"}
        layout.addView(b1)
        layout.addView(b2)
        b1.layoutParams
        return layout
    }

}