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
    private val copy = tag.copy(isFavorite = value)
    override val showSnackbarDefault = true
    override fun getUndoMessage(context: Context): String {
        return if (value) context.getString(R.string.unfavorite_tag_with_name, tag.name)
        else context.getString(R.string.favorite_tag_with_name, tag.name)
    }

    override suspend fun run(context: Context): Boolean {
        return withContext(TagDispatcher) {
            context.db.tags.replaceElement(tag, copy)
            return@withContext true
        }
    }

    override suspend fun undo(context: Context): Boolean {
        return withContext(TagDispatcher) {
            if (tag.isFavorite) {
                context.db.tags.replaceElement(copy, tag)
                true
            } else false
        }
    }
}

class CommandUpdateFollowingTagData(private val tag: Tag, private val following: Following?) : Command {
    private val copy = tag.copy(following = following)

    override val showSnackbarDefault = true
    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.undo_updating_tag_with_name, tag.name)
    }

    override suspend fun run(context: Context): Boolean {
        return withContext(TagDispatcher) {
            context.db.tags.replaceElement(tag, copy)
            true
        }
    }

    override suspend fun undo(context: Context): Boolean {
        return withContext(TagDispatcher) {
            context.db.tags.replaceElement(copy, tag)
            true
        }
    }
}

class CommandUpdateSeveralFollowingTagData(private val tagFollowingDataPairs: List<Pair<Tag, Following?>>) : Command {
    private val copy = tagFollowingDataPairs.map { Pair(it.first, it.first.copy(following = it.second)) }

    override val showSnackbarDefault = true
    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.undo_updating_tags)
    }

    override suspend fun run(context: Context): Boolean {
        return withContext(TagDispatcher) {
            copy.forEach { context.db.tags.replaceElement(it.first, it.second) }
            true
        }
    }

    override suspend fun undo(context: Context): Boolean {
        return withContext(TagDispatcher) {
            copy.forEach { context.db.tags.replaceElement(it.second, it.first) }
            true
        }
    }
}