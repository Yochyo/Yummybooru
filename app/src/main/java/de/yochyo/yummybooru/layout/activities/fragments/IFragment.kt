package de.yochyo.yummybooru.layout.activities.fragments

import android.view.ViewGroup

interface IFragment {
    fun withContainer(task: (container: ViewGroup) -> Unit)
}