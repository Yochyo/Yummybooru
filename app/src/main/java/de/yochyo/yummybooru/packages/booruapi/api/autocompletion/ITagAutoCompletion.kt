package de.yochyo.booruapi.api.autocompletion

import de.yochyo.booruapi.api.IBooruApi
import de.yochyo.booruapi.api.Tag

interface ITagAutoCompletion<A : IBooruApi, T : Tag> {
    suspend fun getTagAutoCompletion(api: A, begin: String, limit: Int): List<T>?
}