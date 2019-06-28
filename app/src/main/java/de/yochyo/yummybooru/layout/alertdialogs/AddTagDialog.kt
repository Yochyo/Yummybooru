package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.entities.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AddTagDialog(val runOnPositive: (editText: AutoCompleteTextView) -> Unit) {
    var title: String = ""

    fun build(context: Context): AlertDialog {
        var dialogIsDismissed = false
        var clickedDropdown = false

        val builder = AlertDialog.Builder(context)
        val layout = LayoutInflater.from(context).inflate(R.layout.add_tag_dialog_view, null) as LinearLayout
        val editText = layout.findViewById<AutoCompleteTextView>(R.id.add_tag_edittext)
        val arrayAdapter = object : ArrayAdapter<Tag>(context, android.R.layout.simple_dropdown_item_1line) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tag = getItem(position)
                val textView = super.getView(position, convertView, parent) as TextView
                if (tag != null) {
                    if (Build.VERSION.SDK_INT > 22) textView.setTextColor(context.getColor(tag.color))
                    else textView.setTextColor(context.resources.getColor(tag.color))
                }
                return textView
            }
        }
        editText.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id -> clickedDropdown = true }
        editText.setAdapter(arrayAdapter)
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                editText.setAdapter(arrayAdapter) //Because of bug, that suggestions aren´t correctly updated
                val name = s.toString()
                GlobalScope.launch {
                    val tags = Api.searchTags(name)
                    launch(Dispatchers.Main) {
                        if (!dialogIsDismissed && editText.text.toString() == name) {
                            arrayAdapter.apply { clear(); addAll(tags); notifyDataSetChanged() }
                            if (clickedDropdown || (tags.size == 1 && tags.first().name == name)) {
                                editText.dismissDropDown()
                                clickedDropdown = false
                            } else {
                                editText.showDropDown()
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