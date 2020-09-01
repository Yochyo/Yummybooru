package de.yochyo.yummybooru.layout.activities.followingactivity

import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import kotlinx.coroutines.*

class FollowingCountUtil(val activity: FollowingActivity) {
    var adapter: RecyclerView.Adapter<FollowingTagViewHolder>? = null
    private val counts = HashMap<String, Int>()
    private val mutex = Any()

    var paused = false
    private val job = GlobalScope.launch(Dispatchers.IO) {
        while (isActive) {
            try {
                for (i in activity.layoutManager.findFirstVisibleItemPosition()..activity.layoutManager.findLastVisibleItemPosition()) {
                    val follow = activity.filteringFollowingList.elementAt(i)
                    cacheCount(follow.name)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val onAddElementListener = Listener.create<OnAddElementsEvent<Tag>> { GlobalScope.launch { it.elements.forEach { element -> cacheCount(element.name) } } }

    init {
        activity.db.tags.registerOnAddElementsListener(onAddElementListener)
    }

    fun getCount(tag: Tag): Int {
        GlobalScope.launch { cacheCount(tag.name) }
        val countDifference = getRawCount(tag.name) - (tag.following?.lastCount ?: 0)
        return if (countDifference > 0) countDifference else 0
    }

    private suspend fun cacheCount(name: String): Int {
        return withContext(Dispatchers.IO) {
            var newCount = 0
            try {
                val oldValue = getRawCount(name)
                val t = activity.db.currentServer.getTag(activity, name)
                newCount = t?.count ?: 0
                setCount(name, newCount)
                if (oldValue != newCount) {
                    val newIndex = activity.filteringFollowingList.indexOfFirst { it.name == name }
                    if (newIndex >= 0)
                        withContext(Dispatchers.Main) { adapter?.notifyItemChanged(newIndex) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            newCount
        }
    }

    private fun setCount(name: String, count: Int) {
        synchronized(mutex) {
            counts[name] = count
        }
    }

    private fun getRawCount(name: String): Int {
        return synchronized(mutex) {
            counts[name] ?: 0
        }
    }

    fun close() {
        activity.db.tags.removeOnAddElementsListener(onAddElementListener)
        job.cancel()
    }
}