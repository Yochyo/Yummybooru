package de.yochyo.yummybooru.layout.activities.followingactivity

import android.content.Context
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventmanager.EventHandler
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import kotlinx.coroutines.*
import java.io.Closeable
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max

class FollowingObservers : Closeable {
    var paused = false

    private val observers = HashMap<String, FollowingObserver>()

    fun createObserver(context: Context, server: Server, tag: Tag): FollowingObserver {
        val observer = observers[tag.name] ?: FollowingObserver(context, server, tag)
        observers[tag.name] = observer
        return observer
    }

    override fun close() {
        observers.forEach { it.value.close() }
    }

    inner class FollowingObserver(val context: Context, val server: Server, val tag: Tag) : Closeable {
        //NewTagCount, TagCountDifference
        var value = 0
        private val onChange = EventHandler<OnChangeObjectEvent<Int, Int>>()
        private var job: Job? = null

        suspend fun updateCountDifference() {
            withContext(Dispatchers.IO) {
                val tag = context.db.currentServer.getTag(context, tag.name)
                val value = max(0, tag.count - (this@FollowingObserver.tag.lastCount ?: 0))
                this@FollowingObserver.value = value
                onChange.trigger(OnChangeObjectEvent(tag.count, value))
            }
        }

        fun setListener(listener: Listener<OnChangeObjectEvent<Int, Int>>) {
            onChange.removeAllListeners()
            onChange.registerListener(listener)
        }

        override fun close() {
            job?.cancel()
            job = null
        }

        fun start() {
            if (job == null) {
                job = GlobalScope.launch(Dispatchers.IO) {
                    while (isActive) {
                        if (!paused && !this@FollowingObservers.paused)
                            updateCountDifference()
                        delay(30000)
                    }
                }
            }
        }
    }
}