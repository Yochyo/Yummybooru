package de.yochyo.yummybooru.utils.commands

import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface Command {
    companion object {
        fun execute(view: View, command: Command, showSnackbar: Boolean = command.showSnackbarDefault) {
            GlobalScope.launch {
                if (command.run(view.context) && showSnackbar) {
                    val snack = Snackbar.make(view, command.getUndoMessage(view.context), 2500)
                    snack.setAction("Undo") { GlobalScope.launch { command.undo(view.context) } }
                    withContext(Dispatchers.Main) { snack.show() }
                }
            }
        }
    }

    val showSnackbarDefault: Boolean
    fun getUndoMessage(context: Context): String
    fun run(context: Context): Boolean
    fun undo(context: Context): Boolean
}

fun Command.execute(view: View, showSnackbar: Boolean = showSnackbarDefault) = Command.execute(view, this, showSnackbar)