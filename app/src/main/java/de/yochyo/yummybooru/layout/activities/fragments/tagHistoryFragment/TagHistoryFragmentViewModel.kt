package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment

import android.content.Context
import androidx.lifecycle.*
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.LiveDataValue

class TagHistoryFragmentViewModel : ViewModel() {
    private lateinit var allTags: LiveData<List<Tag>>

    val selectedTags = MutableLiveData<List<String>>(emptyList())
    val filter = MutableLiveData("")
    lateinit var tags: LiveData<List<Tag>>
    lateinit var server: Server
    lateinit var selectedTagsValue: LiveDataValue<List<String>>

    fun init(owner: LifecycleOwner, context: Context) {
        selectedTagsValue = LiveDataValue(selectedTags, owner)
        server = context.db.selectedServerValue
        allTags = context.db.tags
        tags = getTagMediator()

        allTags.observe(owner, Observer { tags ->
            selectedTags.value = selectedTags.value?.filter { selectedTag -> tags.find { it.name == selectedTag } != null }
        })
    }

    private fun getTagMediator(): LiveData<List<Tag>> {
        return MediatorLiveData<List<Tag>>().apply {
            fun update() {
                val filter = filter.value ?: return
                val tags = allTags.value ?: return
                value = tags.filter { it.name.contains(filter) }
            }
            addSource(filter) { update() }
            addSource(allTags) { update() }
        }
    }
}