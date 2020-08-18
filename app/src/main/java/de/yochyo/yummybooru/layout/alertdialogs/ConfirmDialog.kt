package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import de.yochyo.yummybooru.R

class ConfirmDialog(val runOnPositive: () -> Unit) {
    var title: String? = null
    var message = ""

    fun withMessage(s: String) = apply { message = s }
    fun withTitle(s: String) = apply { title = s }

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title ?: context.getString(R.string.confirm))
        if (message != "") builder.setMessage(message)

        builder.setPositiveButton(context.getString(R.string.positive_button_name)) { _, _ ->
            runOnPositive()
        }
        builder.show()
    }
}