package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.utils.general.currentServer
import de.yochyo.yummybooru.utils.general.setColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AddTagDialog(val runOnPositive: (editText: AutoCompleteTextView) -> Unit) {
    var title: String = "Add tag"
    var tagName: String = ""

    fun withTag(tag: String) = apply { tagName = tag }
    fun withTitle(s: String) = apply { title = s }

    fun build(context: Context): AlertDialog {
        var dialogIsDismissed = false
        var clickedDropdown = false

        val builder = AlertDialog.Builder(context)
        val layout = LayoutInflater.from(context).inflate(R.layout.add_tag_dialog_view, null) as LinearLayout
        val editText = layout.findViewById<AutoCompleteTextView>(R.id.add_tag_edittext)
        (editText as TextView).text = tagName
        val arrayAdapter = object : ArrayAdapter<Tag>(context, android.R.layout.simple_dropdown_item_1line) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tag = getItem(position)
                val textView = super.getView(position, convertView, parent) as TextView
                if (tag != null)
                    textView.setColor(tag.color)

                return textView
            }

        }
        editText.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ -> clickedDropdown = true }
        editText.setAdapter(arrayAdapter)
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                editText.setAdapter(arrayAdapter) //Because of bug, that suggestions aren´t correctly updated
                val string = s.toString()
                val name = if (string.contains(" ")) string.split(" ").last() else string
                GlobalScope.launch {
                    val lastIndexOf = string.lastIndexOf(" ")
                    val a = if (lastIndexOf != -1) string.substring(0..lastIndexOf) else ""
                    val tags = context.currentServer.getMatchingTags(context, name)?.map { Tag(a + it.name, it.type) } //damit der filter funktioniert
                    if (tags != null) {
                        launch(Dispatchers.Main) {
                            if (!dialogIsDismissed) {
                                arrayAdapter.apply { clear(); addAll(tags); notifyDataSetChanged() }
                                if (clickedDropdown || (tags.size == 1 && tags.first().name == string)) {
                                    editText.dismissDropDown()
                                    clickedDropdown = false
                                } else editText.showDropDown()
                            }
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                editText.dismissDropDown()
                editText.setAdapter(null)//Because of bug, that suggestions aren´t correctly updated
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })
        builder.setMessage(title).setView(layout)
        builder.setPositiveButton("OK") { _, _ ->
            runOnPositive(editText)
        }
        builder.setNeutralButton("Search") { _, _ ->
            PreviewActivity.startActivity(context, editText.text.toString())
        }

        val dialog = builder.create()
        dialog.show()
        editText.requestFocus()
        dialog.setOnDismissListener { dialogIsDismissed = true }
        dialog.window?.setGravity(Gravity.TOP)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        return dialog
    }
}