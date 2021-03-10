package de.yochyo.yummybooru.utils.commands

import android.content.Context
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db

class CommandAddServer(val server: Server) : Command {
    private lateinit var copy: Server
    override val show: Command.Show = Command.Show.TOAST

    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.delete_server_with_name, server.name)

    }

    override fun getToastMessage(context: Context): String {
        return context.getString(R.string.added_server_with_name, server.name)
    }

    override fun run(context: Context): Boolean {
        copy = server.copy(id = context.db.addServer(server).toInt())
        return copy.id > 0
    }

    override fun undo(context: Context): Boolean {
        return context.db.removeServer(copy) > 0
    }
}

class CommandDeleteServer(val server: Server) : Command {
    override val show: Command.Show = Command.Show.SNACKBAR

    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.add_server_with_name, server.name)

    }

    override fun getToastMessage(context: Context): String {
        return context.getString(R.string.added_server_with_name, server.name)
    }

    override fun run(context: Context): Boolean {
        return context.db.removeServer(server) > 0
    }

    override fun undo(context: Context): Boolean {
        return context.db.addServer(server) > 0
    }
}

class CommandUpdateServer(val server: Server, val new: Server) : Command {
    override val show: Command.Show = Command.Show.TOAST

    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.revert_server_with_name, server.name)

    }

    override fun getToastMessage(context: Context): String {
        return context.getString(R.string.updated_server_with_name, server.name)
    }

    override fun run(context: Context): Boolean {
        return context.db.updateServer(new) > 0
    }

    override fun undo(context: Context): Boolean {
        return context.db.updateServer(server) > 0
    }
}