package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LifecycleOwner
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.TagHistoryFragmentViewModel
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview_with_tag_collections.TagCollectionExpandableGroup
import de.yochyo.yummybooru.layout.alertdialogs.TagHistoryCollectionEditDialog
import de.yochyo.yummybooru.utils.withValue


class TagCollectionComponent(val viewModel: TagHistoryFragmentViewModel, val viewForSnack: View, container: ViewGroup, owner: LifecycleOwner) {
    companion object {
        const val ANIMATION_DURATION = 300L
    }

    lateinit var collection: TagCollectionExpandableGroup
    val toolbar: Toolbar = LayoutInflater.from(viewForSnack.context).inflate(R.layout.tag_collection_toolbar, container, false) as Toolbar
    val arrow = toolbar.findViewById<ImageView>(R.id.arrow)

    init {
        toolbar.inflateMenu(R.menu.tag_collection_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit_tag_collection -> viewModel.allTagsSorted.withValue(owner) { TagHistoryCollectionEditDialog(it, collection.toEntity()).build(viewForSnack.context) }
                R.id.delete_tag_collection -> TODO()
            }
            true
        }
    }

    fun animateExpand() {
        val rotate = RotateAnimation(360f, 180f, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f)
        rotate.duration = ANIMATION_DURATION
        rotate.fillAfter = true
        arrow.animation = rotate
    }

    fun animateCollapse() {
        val rotate = RotateAnimation(180f, 360f, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f)
        rotate.duration = ANIMATION_DURATION
        rotate.fillAfter = true
        arrow.animation = rotate
    }
}