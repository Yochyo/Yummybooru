package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment

import android.content.Context
import androidx.lifecycle.*
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.database.entities.TagCollectionWithTags
import de.yochyo.yummybooru.utils.LiveDataValue
import de.yochyo.yummybooru.utils.TagUtil

class TagHistoryFragmentViewModel : ViewModel() {
    private lateinit var allTags: LiveData<List<Tag>>
    private lateinit var allCollections: LiveData<List<TagCollectionWithTags>>
    private lateinit var tagSortComparator: LiveData<Comparator<Tag>>

    lateinit var server: Server
    val filter = MutableLiveData("")
    val selectedTags = MutableLiveData<List<String>>(emptyList())
    lateinit var selectedTagsValue: LiveDataValue<List<String>>

    //TagHistoryFragment
    lateinit var tags: LiveData<List<Tag>>

    //TagHistoryCollectionFragment
    lateinit var collections: LiveData<List<TagCollectionWithTags>>
    lateinit var allTagsSorted: LiveData<List<Tag>>


    fun init(owner: LifecycleOwner, context: Context) {
        selectedTagsValue = LiveDataValue(selectedTags, owner)
        server = context.db.selectedServerValue
        allTags = context.db.tags
        allCollections = context.db.tagCollections
        tagSortComparator = TagUtil.getTagComparatorLiveData(context)

        tags = getTagMediator()
        collections = getTagCollectionMediator()
        allTagsSorted = getAllTagsSortedMediator()
        allTags.observe(owner, Observer { tags ->
            selectedTags.value = selectedTags.value?.filter { selectedTag -> tags.find { it.name == selectedTag } != null }
        })
    }


    private fun getTagCollectionMediator(): LiveData<List<TagCollectionWithTags>> {
        return MediatorLiveData<List<TagCollectionWithTags>>().apply {
            fun update() {
                val filter = filter.value ?: return
                val comparator = tagSortComparator.value ?: return
                val collections = allCollections.value ?: return
                val tags = allTags.value ?: return

                val c = (collections.map { it } as MutableList<TagCollectionWithTags>).apply { add(TagCollectionWithTags("-", server.id).copy(tags = tags)) }
                value = c.filter { it.collection.name.contains(filter) || it.tags.find { it.name.contains(filter) } != null }
                    .map { it.copy(tags = it.tags.filter { it.name.contains(filter) }.sortedWith(comparator)) }
            }
            addSource(filter) { update() }
            addSource(allCollections) { update() }
            addSource(tagSortComparator) { update() }
            addSource(allTags) { update() }
        }
    }

    private fun getAllTagsSortedMediator(): LiveData<List<Tag>> {
        return MediatorLiveData<List<Tag>>().apply {
            fun update() {
                val tags = allTags.value ?: return
                val comparator = tagSortComparator.value ?: return
                value = tags.sortedWith(comparator)
            }
            addSource(allTags) { update() }
            addSource(tagSortComparator) { update() }
        }
    }

    private fun getTagMediator(): LiveData<List<Tag>> {
        return MediatorLiveData<List<Tag>>().apply {
            fun update() {
                val filter = filter.value ?: return
                val tags = allTags.value ?: return
                val comparator = tagSortComparator.value ?: return
                value = tags.filter { it.name.contains(filter) }.sortedWith(comparator)
            }
            addSource(filter) { update() }
            addSource(allTags) { update() }
            addSource(tagSortComparator) { update() }
        }
    }
}