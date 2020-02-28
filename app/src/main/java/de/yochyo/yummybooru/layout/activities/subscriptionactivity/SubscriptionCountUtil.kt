package de.yochyo.yummybooru.layout.activities.subscriptionactivity

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.entities.Subscription
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
                    val sub = activity.currentFilter[i]
                    cacheCount(sub.name)
                }
            }
        }
    }

    private val onAddElementListener = Listener.create<EventCollection<Subscription>.OnAddElementEvent> { event -> GlobalScope.launch { cacheCount(event.element.name) } }

    init {
        activity.db.subs.onAddElement.registerListener(onAddElementListener)
    }

    fun getCount(sub: Subscription): Int {
        GlobalScope.launch { cacheCount(sub.name) }
        val countDifference = getRawCount(sub.name) - sub.lastCount
        return if (countDifference > 0) countDifference else 0
    }

    private suspend fun cacheCount(name: String): Int {
        return withContext(Dispatchers.IO) {
            var newCount = 0
            tryCatchSuspended {
                val oldValue = getRawCount(name)
                val tag = Api.getTag(activity, name)
                newCount = tag.count
                setCount(name, newCount)
                if (oldValue != newCount) {
                    val newIndex = activity.currentFilter.indexOfFirst { it.name == name }
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
        activity.db.subs.onAddElement.removeListener(onAddElementListener)
        job.cancel()
    }
}