package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.updater.Changelog

class ShowChangelogsDialog {
    private val changelogs = ArrayList<Changelog>()
    fun withChangelogs(logs: Collection<Changelog>) = apply { changelogs += logs }

    fun build(context: Context){
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Changelogs")
        builder.setView(createLayout(context))
        builder.setPositiveButton("Ok"){ _,_ -> }
        builder.show()
    }


    private fun createLayout(context: Context): View{
        val scroll = LayoutInflater.from(context).inflate(R.layout.changelogs_dialog_layout, null)
        val layout = scroll.findViewById<LinearLayout>(R.id.layout)

        for(log in changelogs){
            val child = LayoutInflater.from(context).inflate(R.layout.changelogs_dialog_layout_item, null)
            child.findViewById<TextView>(R.id.title).text = log.versionName
            child.findViewById<TextView>(R.id.description).text = log.description
            layout.addView(child)
        }

        return scroll
    }
}