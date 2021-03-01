package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import de.yochyo.booruapi.api.TagType
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandAddTag

class AddSpecialTagDialog {
    var title: String? = null
    fun withTitle(s: String) = apply { title = s }

    fun build(snackbarView: View, server: Server) {
        val builder = AlertDialog.Builder(snackbarView.context)
        val layout = LayoutInflater.from(snackbarView.context).inflate(R.layout.main_add_special_tag_layout, null)
        builder.setTitle(title)
        builder.setView(layout)

        layout.findViewById<ImageView>(R.id.height_add).setOnClickListener {
            val text = layout.findViewById<TextView>(R.id.height_edittext).text
            if (text.isNotEmpty()) {
                val stringBuilder = StringBuilder()
                stringBuilder.append("height")
                stringBuilder.append(layout.findViewById<Spinner>(R.id.height_spinner).selectedItem.toString())
                stringBuilder.append(text)
                val tag = Tag(stringBuilder.toString(), TagType.UNKNOWN, server.id)
                Command.execute(snackbarView, CommandAddTag(tag))
            }
        }
        layout.findViewById<ImageView>(R.id.width_add).setOnClickListener {
            val text = layout.findViewById<TextView>(R.id.width_edittext).text
            if(text.isNotEmpty()) {
                val stringBuilder = StringBuilder()
                stringBuilder.append("width")
                stringBuilder.append(layout.findViewById<Spinner>(R.id.width_spinner).selectedItem.toString())
                stringBuilder.append(text)
                val tag = Tag(stringBuilder.toString(), TagType.UNKNOWN, server.id)
                Command.execute(snackbarView, CommandAddTag(tag))
            }
        }

        builder.show()
    }
}