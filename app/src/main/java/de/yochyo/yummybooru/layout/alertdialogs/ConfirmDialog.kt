package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.utils.general.Configuration

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
        val dialog = builder.create()
        dialog.window.apply { if (this != null) Configuration.setWindowSecurityFrag(context, this) }
        dialog.show()
    }
}