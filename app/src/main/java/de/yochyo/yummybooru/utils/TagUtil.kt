package de.yochyo.yummybooru.utils

import android.content.Context
import android.view.View
import androidx.lifecycle.*
import de.yochyo.booruapi.api.TagType
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.*
import de.yochyo.yummybooru.utils.commands.*
import de.yochyo.yummybooru.utils.enums.TagSortType
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.coroutines.*
import java.util.ArrayList
import kotlin.Boolean
import kotlin.Comparator
import kotlin.String
import kotlin.apply

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
            if (tag == null) GlobalScope.launch(Dispatchers.IO) {
                CommandAddTag(viewForSnackbar.db.selectedServerValue.getTag(name).copy(following = getFollowingData(viewForSnackbar.context, name))).execute(viewForSnackbar)
            } else followOrUnfollow(viewForSnackbar, tag)
        })
    }

    fun getTagComparator(context: Context): Comparator<Tag> {
        fun TagType.toInt() = when (this) {
            TagType.ARTIST -> 0
            TagType.COPYRIGHT -> 1
            TagType.CHARACTER -> 2
            TagType.GENERAL -> 3
            TagType.META -> 4
            TagType.UNKNOWN -> 5
        }

        val comparatorChain = ArrayList<Comparator<Tag>>()
        if (context.preferences.sortTagsByFavoriteFirst) comparatorChain += Comparator { o1, o2 ->
            if (o1.isFavorite == o2.isFavorite) 0
            else if (o1.isFavorite && !o2.isFavorite) -1
            else 1
        }

        if (context.preferences.sortTagsByTagType) comparatorChain += Comparator { o1, o2 -> o1.type.toInt().compareTo(o2.type.toInt()) }
        comparatorChain += when (context.preferences.tagSortType) {
            TagSortType.NAME_ASC -> Comparator { o1, o2 -> o1.name.compareTo(o2.name) }
            TagSortType.NAME_DES -> Comparator { o1, o2 -> o2.name.compareTo(o1.name) }
            TagSortType.DATE_ASC -> Comparator { o1, o2 -> o2.creation.compareTo(o1.creation) }
            TagSortType.DATE_DES -> Comparator { o1, o2 -> o2.creation.compareTo(o1.creation) }
        }
        return Comparator { o1, o2 ->
            var res = 0
            for (comparator in comparatorChain) {
                res = comparator.compare(o1, o2)
                if (res != 0) break
            }
            res
        }
    }

    fun getTagComparatorLiveData(context: Context): LiveData<Comparator<Tag>> {
        val sortTagsByFavoriteFirst = context.preferences.prefs.booleanLiveData(
            context.getString(R.string.show_favorites_first),
            context.resources.getBoolean(R.bool.show_favorites_first_default_value)
        ).distinctUntilChanged()
        val sortTagsByTagType = context.preferences.prefs.booleanLiveData(
            context.getString(R.string.sort_by_tag_type),
            context.resources.getBoolean(R.bool.sort_by_tag_type_default_value)
        ).distinctUntilChanged()
        val tagSortType = context.preferences.prefs.stringLiveData(
            context.getString(R.string.sort_tag_comparator), context.resources.getString(R.string.sort_tag_comparator_default_value)
        ).distinctUntilChanged()
        return MediatorLiveData<Comparator<Tag>>().apply {
            fun update() {
                val sortTagsByFavoriteFirstValue = sortTagsByFavoriteFirst.value ?: return
                val sortTagsByTagTypeValue = sortTagsByTagType.value ?: return
                val tagSortTypeValue = tagSortType.value ?: return
                value = getTagComparator(context)
            }
            addSource(sortTagsByFavoriteFirst) { update() }
            addSource(sortTagsByTagType) { update() }
            addSource(tagSortType) { update() }
        }
    }

    private suspend fun getFollowingData(context: Context, name: String): Following? {
        return withContext(Dispatchers.IO) {
            val s = context.db.selectedServerValue
            val tagAsync = GlobalScope.async { s.getTag(name) }
            val id = s.newestID()
            if (id != null) Following(id, tagAsync.await().count)
            else null
        }
    }

}