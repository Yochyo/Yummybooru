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
        suspend fun execute(view: View, command: Command, showSnackbar: Boolean = command.showSnackbarDefault): Boolean {
            val res = command.run(view.context)
            if (res) {
                val snack = Snackbar.make(view, command.getUndoMessage(view.context), 2500)
                if (showSnackbar) {
                    snack.setAction("Undo") { GlobalScope.launch { command.undo(view.context) } }
                    withContext(Dispatchers.Main) { snack.show() }
                }
            }
            return res
        }
    }

    val showSnackbarDefault: Boolean
    fun getUndoMessage(context: Context): String
    suspend fun run(context: Context): Boolean
    suspend fun undo(context: Context): Boolean
}