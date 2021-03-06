package de.yochyo.yummybooru.utils.commands

import android.content.Context
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.database.entities.TagCollection
import de.yochyo.yummybooru.database.entities.TagCollectionWithTags

class CommandDeleteTagCollection(val collection: TagCollectionWithTags) : Command {
    override val show: Command.Show = Command.Show.SNACKBAR

    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.add_tag_collection_with_name, collection.collection.name)
    }

    override fun getToastMessage(context: Context): String {
        return context.getString(R.string.deleted_tag_collection_with_name, collection.collection.name)
    }

    override fun run(context: Context): Boolean {
        return context.db.removeTagCollection(collection.collection) > 0
    }

    override fun undo(context: Context): Boolean {
        val res = context.db.addTagCollection(collection.collection) > 0
        context.db.addTagsToCollection(collection.collection, collection.tags)
        return res
    }
}

class CommandAddTagCollection(val collection: TagCollection) : Command {
    private var result: TagCollection? = null
    override val show: Command.Show = Command.Show.TOAST

    override fun getUndoMessage(context: Context): String {
        return context.getString(R.string.delete_tag_collection_with_name, collection.name)
    }

    override fun getToastMessage(context: Context): String {
        return context.getString(R.string.added_tag_collection_with_name, collection.name)
    }

    override fun run(context: Context): Boolean {
        val res = context.db.addTagCollection(collection)
        result = collection.copy(id = res.toInt())
        return res > 0
    }

    override fun undo(context: Context): Boolean {
        val res = result ?: return false
        return context.db.removeTagCollection(res) > 0
    }
}