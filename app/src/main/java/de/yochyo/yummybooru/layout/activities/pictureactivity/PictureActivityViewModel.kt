package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db

class PictureActivityViewModel : ViewModel() {
    lateinit var tags: LiveData<List<Tag>>
    lateinit var server: Server
    fun init(context: Context) {
        tags = context.db.tags
        server = context.db.selectedServerValue
    }
}