package de.yochyo.yummybooru.layout.activities.followingactivity

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.TagUtil

class FollowingActivityViewModel : ViewModel() {
    private lateinit var tagComparator: LiveData<Comparator<Tag>>
    private lateinit var _tags: LiveData<List<Tag>>
    lateinit var server: Server
    lateinit var tags: LiveData<List<Tag>>
    var filter = MutableLiveData("")

    fun init(context: Context) {
        tagComparator = TagUtil.getTagComparatorLiveData(context)
        server = context.db.selectedServerValue
        _tags = context.db.tags
        tags = MediatorLiveData<List<Tag>>().apply {
            fun update() {
                val tags = _tags.value ?: return
                val filter = filter.value ?: return
                val comparator = tagComparator.value ?: return
                value = tags.filter { it.following != null && it.name.contains(filter) }.sortedWith(comparator)
            }
            addSource(_tags) { update() }
            addSource(filter) { update() }
            addSource(tagComparator) { update() }
        }
    }
}