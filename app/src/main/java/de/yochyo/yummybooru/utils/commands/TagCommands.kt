package de.yochyo.yummybooru.utils.commands

import android.content.Context
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.TagDispatcher
import kotlinx.coroutines.withContext

class CommandAddTag(private val tag: Tag) : Command {
    override val showSnackbarDefault = false
    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.undo_add_tag_with_name, tag.name)
    }

    override suspend fun run(context: Context): Boolean {
        return withContext(TagDispatcher) { context.db.tags.add(tag) }
    }

    override suspend fun undo(context: Context): Boolean {
        return withContext(TagDispatcher) { context.db.tags.remove(tag) }
    }
}

class CommandDeleteTag(private val tag: Tag) : Command {
    override val showSnackbarDefault = true
    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.add_tag_with_name, tag.name)
    }

    override suspend fun run(context: Context): Boolean {
        return withContext(TagDispatcher) { context.db.tags.remove(tag) }
    }

    override suspend fun undo(context: Context): Boolean {
        return withContext(TagDispatcher) { context.db.tags.add(tag) }
    }
}

class CommandFavoriteTag(private val tag: Tag, private val value: Boolean) : Command {
    override val showSnackbarDefault = true
    override fun getUndoMessage(context: Context): String {
        return if (value) context.getString(R.string.unfavorite_tag_with_name, tag.name)
        else context.getString(R.string.favorite_tag_with_name, tag.name)
    }

    override suspend fun run(context: Context): Boolean {
        return withContext(TagDispatcher) {
            if (tag.isFavorite == value) return@withContext false
            tag.isFavorite = value
            return@withContext true
        }
    }

    override suspend fun undo(context: Context): Boolean {
        return withContext(TagDispatcher) {
            if (tag.isFavorite == value) {
                tag.isFavorite = !value
                true
            } else false
        }
    }
}

class CommandUpdateFollowingTagData(private val tag: Tag, private val following: Following?) : Command {
    private val _following = tag.following

    override val showSnackbarDefault = true
    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.undo_updating_tag_with_name, tag.name)
    }

    override suspend fun run(context: Context): Boolean {
        return withContext(TagDispatcher) {
            tag.following = following
            true
        }
    }

    override suspend fun undo(context: Context): Boolean {
        return withContext(TagDispatcher) {
            tag.following = _following
            true
        }
    }
}

class CommandUpdateSeveralFollowingTagData(private val tagFollowingDatapairs: List<Pair<Tag, Following?>>) : Command {
    private val _following = tagFollowingDatapairs.map { Pair(it.first, it.first.following) }

    override val showSnackbarDefault = true
    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.undo_updating_tags)
    }

    override suspend fun run(context: Context): Boolean {
        return withContext(TagDispatcher) {
            tagFollowingDatapairs.forEach { it.first.following = it.second }
            true
        }
    }

    override suspend fun undo(context: Context): Boolean {
        return withContext(TagDispatcher) {
            _following.forEach { it.first.following = it.second }
            true
        }
    }
}