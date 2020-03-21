package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.StringBuilder

class AddSpecialTagDialog {
    var title = "Tag filter"
    fun withTitle(s: String) = apply { title = s }

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        val layout = LayoutInflater.from(context).inflate(R.layout.main_add_special_tag_layout, null)
        builder.setTitle(title)
        builder.setView(layout)

        layout.findViewById<ImageView>(R.id.height_add).setOnClickListener {
            val text = layout.findViewById<TextView>(R.id.height_edittext).text
            if(text.isNotEmpty()){
                val stringBuilder = StringBuilder()
                stringBuilder.append("height")
                stringBuilder.append(layout.findViewById<Spinner>(R.id.height_spinner).selectedItem.toString())
                stringBuilder.append(text)
                val tag = Tag(stringBuilder.toString(), Tag.SPECIAL)
                context.db.tags += tag
            }
        }
        layout.findViewById<ImageView>(R.id.width_add).setOnClickListener {
            val text = layout.findViewById<TextView>(R.id.width_edittext).text
            if(text.isNotEmpty()) {
                val stringBuilder = StringBuilder()
                stringBuilder.append("width")
                stringBuilder.append(layout.findViewById<Spinner>(R.id.width_spinner).selectedItem.toString())
                stringBuilder.append(text)
                val tag = Tag(stringBuilder.toString(), Tag.SPECIAL)
                context.db.tags += tag
            }
        }

        builder.show()
    }
}