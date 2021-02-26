package de.yochyo.yummybooru.layout.activities.followingactivity

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db

class FollowingActivityViewModel : ViewModel() {
    lateinit var server: Server
    lateinit var tags: LiveData<List<Tag>>
    var filter = MutableLiveData("")

    fun init(context: Context) {
        server = context.db.selectedServerValue
        tags = context.db.tags.map { it.filter { e -> e.following != null } }
    }
}