package de.yochyo.yummybooru.layout.activities.fragments

import android.view.ViewGroup

interface IFragmentWithContainer {
    fun withContainer(task: (container: ViewGroup) -> Unit)
}