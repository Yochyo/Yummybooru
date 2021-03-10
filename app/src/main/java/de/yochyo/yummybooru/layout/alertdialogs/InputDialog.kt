package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import de.yochyo.yummybooru.R

class InputDialog(val runOnPositive: (text: String) -> Unit) {
    var title: String? = null
    var hint = ""

    fun withTitle(s: String) = apply { title = s }
    fun withHint(s: String) = apply { hint = s }

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title ?: context.getString(R.string.confirm))
        val layout = LayoutInflater.from(context).inflate(R.layout.input_dialog_layout, null, false)
        val text = layout.findViewById<EditText>(R.id.edittext)
        text.hint = hint
        builder.setView(layout)

        builder.setPositiveButton(context.getString(R.string.positive_button_name)) { _, _ ->
            runOnPositive(text.text.toString())
        }
        builder.show()
    }
}