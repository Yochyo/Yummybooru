package de.yochyo.yummybooru.utils

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleOwner
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.commands.*
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object TagUtil {
    fun addTag(snackbarView: View, name: String, favorite: Boolean) {
        GlobalScope.launch {
            Command.execute(snackbarView, CommandAddTag(snackbarView.db.selectedServerValue.getTag(name).copy(isFavorite = favorite)))
        }
    }

    fun addTagOrDeleteIfExists(snackBarView: View, owner: LifecycleOwner, name: String, favorite: Boolean = false) {
        snackBarView.context.db.tags.withValue(owner) {
            val tag = it.find { it.name == name }
            if (tag == null) addTag(snackBarView, name, favorite)
            else Command.execute(snackBarView, CommandDeleteTag(tag))
        }
    }

    fun favoriteOrCreateTagIfNotExist(snackbarView: View, owner: LifecycleOwner, name: String) {
        snackbarView.db.tags.withValue(owner, {
            val tag = it.find { it.name == name }
            if (tag == null) addTag(snackbarView, name, true)
            else CommandFavoriteTag(tag, !tag.isFavorite).execute(snackbarView)
        })
    }

    fun favoriteOrUnfavorite(snackBarView: View, owner: LifecycleOwner, name: String) {
        snackBarView.context.db.tags.withValue(owner) {
            favoriteOrUnfavorite(snackBarView, it.find { it.name == name } ?: return@withValue)
        }
    }

    fun favoriteOrUnfavorite(snackBarView: View, tag: Tag) {
        Command.execute(snackBarView, CommandFavoriteTag(tag, !tag.isFavorite))
    }

    fun followOrUnfollow(snackBarView: View, owner: LifecycleOwner, name: String) {
        snackBarView.context.db.tags.withValue(owner, {
            val tag = it.findLast { it.name == name } ?: return@withValue
            followOrUnfollow(snackBarView, tag)
        })
    }

    fun followOrUnfollow(snackBarView: View, tag: Tag) {
        GlobalScope.launch {
            Command.execute(snackBarView, CommandUpdateFollowingTagData(tag, if (tag.following == null) getFollowingData(snackBarView.context, tag.name) else null))
        }
    }

    fun CreateFollowedTagOrChangeFollowing(viewForSnackbar: View, owner: LifecycleOwner, name: String) {
        viewForSnackbar.db.tags.withValue(owner, {
            val tag = it.find { it.name == name }
            if (tag == null) GlobalScope.launch {
                CommandAddTag(viewForSnackbar.db.selectedServerValue.getTag(name).copy(following = getFollowingData(viewForSnackbar.context, name)))
            } else followOrUnfollow(viewForSnackbar, tag)
        })
    }

    private suspend fun getFollowingData(context: Context, name: String): Following? {
        val s = context.db.selectedServerValue
        val tagAsync = GlobalScope.async { s.getTag(name) }
        val id = s.newestID()
        return if (id != null) Following(id, tagAsync.await().count)
        else null
    }

}