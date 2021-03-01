package de.yochyo.yummybooru.utils.commands

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface Command {

    enum class Show {
        NONE, SNACKBAR, TOAST;
    }

    companion object {
        fun execute(view: View, command: Command, showSnackbar: Show = command.show) {
            GlobalScope.launch(Dispatchers.IO) { executeAsync(view, command, showSnackbar) }
        }

        suspend fun executeAsync(view: View, command: Command, showSnackbar: Show = command.show): Boolean {
            return withContext(Dispatchers.IO) {
                val res = command.run(view.context)
                if (res) {
                    withContext(Dispatchers.Main) {
                        when (showSnackbar) {
                            Show.SNACKBAR -> {
                                val snack = Snackbar.make(view, command.getUndoMessage(view.context), 2500)
                                snack.setAction("Undo") { command.undo(view.context) }
                                snack.show()
                            }
                            Show.TOAST -> {
                                Toast.makeText(view.context, command.getToastMessage(view.context), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                res
            }
        }
    }

    val show: Show
    fun getUndoMessage(context: Context): String
    fun getToastMessage(context: Context): String
    fun run(context: Context): Boolean
    fun undo(context: Context): Boolean
}

fun Command.execute(view: View, showSnackbar: Command.Show = show) = Command.execute(view, this, show)
suspend fun Command.executeAsync(view: View, showSnackbar: Command.Show = show) = Command.executeAsync(view, this, show)