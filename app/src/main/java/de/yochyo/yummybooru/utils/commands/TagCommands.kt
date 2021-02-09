package de.yochyo.yummybooru.utils.commands

import android.content.Context
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db

class CommandAddTag(private val context: Context, private val tag: Tag) : Command {
    override val undoMessage: String = context.getString(R.string.undo_add_tag_with_name, tag.name)

    override suspend fun run(): Boolean {
        return context.db.tags.add(tag)
    }

    override suspend fun undo(): Boolean {
        return context.db.tags.remove(tag)
    }
}

class CommandDeleteTag(private val context: Context, private val tag: Tag) : Command {
    override val undoMessage: String = context.getString(R.string.add_tag_with_name, tag.name)

    override suspend fun run(): Boolean {
        return context.db.tags.remove(tag)
    }

    override suspend fun undo(): Boolean {
        return context.db.tags.add(tag)
    }
}

class CommandFavoriteTag(private val context: Context, private val tag: Tag, private val value: Boolean) : Command {
    override val undoMessage: String = context.getString(R.string.undo_updating_tag_with_name, tag.name)

    override suspend fun run(): Boolean {
        if (tag.isFavorite == value) return false
        tag.isFavorite = value
        return true
    }

    override suspend fun undo(): Boolean {
        return if (tag.isFavorite == value) {
            tag.isFavorite = !value
            true
        } else false
    }
}

class CommandUpdateFollowedData(private val context: Context, private val tag: Tag, private val following: Following) : Command {
    private val _following = tag.following
    override val undoMessage: String = context.getString(R.string.undo_updating_tag_with_name, tag.name)

    override suspend fun run(): Boolean {
        tag.following = following
        return true
    }

    override suspend fun undo(): Boolean {
        tag.following = _following
        return true
    }
}