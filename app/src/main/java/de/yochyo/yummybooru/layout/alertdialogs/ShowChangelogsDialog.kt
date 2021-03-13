package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.updater.Changelog
import de.yochyo.yummybooru.utils.general.Configuration

class ShowChangelogsDialog {
    private val changelogs = ArrayList<Changelog>()
    fun withChangelogs(logs: Collection<Changelog>) = apply { changelogs += logs }

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.changelogs))
        builder.setView(createLayout(context))
        builder.setPositiveButton(context.getString(R.string.positive_button_name)) { _, _ -> }
        val dialog = builder.create()
        dialog.window.apply { if (this != null) Configuration.setWindowSecurityFrag(context, this) }
        dialog.show()
    }


    private fun createLayout(context: Context): View {
        val scroll = LayoutInflater.from(context).inflate(R.layout.changelogs_dialog_layout, null)
        val layout = scroll.findViewById<LinearLayout>(R.id.layout)

        for (log in changelogs) {
            val child = LayoutInflater.from(context).inflate(R.layout.changelogs_dialog_layout_item, null)
            child.findViewById<TextView>(R.id.title).text = log.versionName
            child.findViewById<TextView>(R.id.description).text = log.description
            layout.addView(child)
        }

        return scroll
    }
}