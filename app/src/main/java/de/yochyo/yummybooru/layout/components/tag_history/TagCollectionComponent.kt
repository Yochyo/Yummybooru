package de.yochyo.yummybooru.layout.components.tag_history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview_with_tag_collections.TagCollectionExpandableGroup


class TagCollectionComponent(val viewForSnack: View, container: ViewGroup) {
    companion object {
        const val ANIMATION_DURATION = 300L
    }

    private lateinit var tag: TagCollectionExpandableGroup
    val toolbar: Toolbar = LayoutInflater.from(viewForSnack.context).inflate(R.layout.tag_collection_toolbar, container, false) as Toolbar
    val arrow = toolbar.findViewById<ImageView>(R.id.arrow) as ImageView


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