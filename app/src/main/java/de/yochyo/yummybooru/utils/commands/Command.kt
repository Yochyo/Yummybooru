package de.yochyo.yummybooru.utils.commands

import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface Command {
    companion object {
        suspend fun execute(view: View, command: Command): Boolean {
            val res = command.run()
            if (res) {
                val snack = Snackbar.make(view, command.undoMessage, 2000)
                snack.setAction("Undo") { GlobalScope.launch { command.undo() } }
                withContext(Dispatchers.Main) { snack.show() }
            }
            return res
        }
    }

    val undoMessage: String
    suspend fun run(): Boolean
    suspend fun undo(): Boolean
}