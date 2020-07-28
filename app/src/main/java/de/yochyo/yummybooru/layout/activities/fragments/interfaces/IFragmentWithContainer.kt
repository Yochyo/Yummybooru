package de.yochyo.yummybooru.layout.activities.fragments.interfaces

import android.view.ViewGroup

interface IFragmentWithContainer {
    fun withContainer(task: (container: ViewGroup) -> Unit)
}