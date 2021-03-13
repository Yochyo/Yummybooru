package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.utils.general.Configuration
import de.yochyo.yummybooru.utils.general.setColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AddTagDialog(val runOnPositive: (tag: String) -> Unit) {
    var title: String? = null
    var tagName: String = ""

    fun withTag(tag: String) = apply { tagName = tag }
    fun withTitle(s: String) = apply { title = s }

    fun build(context: Context): AlertDialog {
        val builder = AlertDialog.Builder(context)
        val layout = LayoutInflater.from(context).inflate(R.layout.add_tag_dialog_view, null) as LinearLayout
        val editText = layout.findViewById<AutoCompleteTextView>(R.id.add_tag_edittext)
        (editText as TextView).text = tagName
        editText.threshold = 1
        editText.setAdapter(createAdapter(context, emptyList()))
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val string = s.toString()
                val name = if (string.contains(" ")) string.split(" ").last() else string
                GlobalScope.launch {
                    val lastIndexOf = string.lastIndexOf(" ")
                    val a = if (lastIndexOf != -1) string.substring(0..lastIndexOf) else ""
                    val tags = context.db.selectedServerValue.getMatchingTags(context, name)?.map { it.copy(name = a + it.name) } ?: return@launch
                    withContext(Dispatchers.Main) {
                        editText.setAdapter(createAdapter(context, tags))
                        editText.showDropDown()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })
        builder.setMessage(title ?: context.getString(R.string.add_tag)).setView(layout)
        builder.setPositiveButton(context.getString(R.string.positive_button_name)) { _, _ ->
            runOnPositive(editText.text.toString())
        }
        builder.setNeutralButton(context.getString(R.string.search_tag)) { _, _ ->
            PreviewActivity.startActivity(context, editText.text.toString())
        }

        val dialog = builder.create()
        dialog.window.apply { if (this != null) Configuration.setWindowSecurityFrag(context, this) }
        dialog.show()
        editText.requestFocus()
        dialog.window?.setGravity(Gravity.TOP)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        return dialog
    }

    private fun createAdapter(context: Context, items: List<Tag>): ArrayAdapter<Tag> {
        val adapter = object : ArrayAdapter<Tag>(context, android.R.layout.simple_dropdown_item_1line) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                try {
                    val tag = getItem(position)
                    val textView = super.getView(position, convertView, parent) as TextView
                    if (tag != null)
                        textView.setColor(tag.color)

                    return textView
                } catch (e: Exception) {
                    e.printStackTrace()
                    return super.getView(position, convertView, parent) as TextView
                }
            }
        }
        adapter.addAll(items)
        return adapter
    }
}