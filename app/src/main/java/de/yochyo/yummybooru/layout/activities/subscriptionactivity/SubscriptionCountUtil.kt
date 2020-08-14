package de.yochyo.yummybooru.layout.activities.subscriptionactivity

import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.tryCatchSuspended
import kotlinx.coroutines.*

class SubscriptionCountUtil(val activity: SubscriptionActivity) {
    var adapter: RecyclerView.Adapter<SubscribedTagViewHolder>? = null
    private val counts = HashMap<String, Int>()
    private val mutex = Any()

    var paused = false
    private val job = GlobalScope.launch(Dispatchers.IO) {
        while (isActive) {
            tryCatchSuspended {
                for (i in activity.layoutManager.findFirstVisibleItemPosition()..activity.layoutManager.findLastVisibleItemPosition()) {
                    val sub = activity.filteringSubList.elementAt(i)
                    cacheCount(sub.name)
                }
            }
        }
    }

    private val onAddElementListener = Listener.create<OnAddElementsEvent<Tag>> { GlobalScope.launch { it.elements.forEach { element -> cacheCount(element.name) } } }

    init {
        activity.db.tags.registerOnAddElementsListener(onAddElementListener)
    }

    fun getCount(tag: Tag): Int {
        GlobalScope.launch { cacheCount(tag.name) }
        val countDifference = getRawCount(tag.name) - (tag.sub?.lastCount ?: 0)
        return if (countDifference > 0) countDifference else 0
    }

    private suspend fun cacheCount(name: String): Int {
        return withContext(Dispatchers.IO) {
            var newCount = 0
            tryCatchSuspended {
                val oldValue = getRawCount(name)
                val t = activity.db.currentServer.getTag(activity, name)
                newCount = t?.count ?: 0
                setCount(name, newCount)
                if (oldValue != newCount) {
                    val newIndex = activity.filteringSubList.indexOfFirst { it.name == name }
                    if (newIndex >= 0)
                        withContext(Dispatchers.Main) { adapter?.notifyItemChanged(newIndex) }
                }
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