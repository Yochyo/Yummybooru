package de.yochyo.yummybooru.downloadservice

import de.yochyo.eventcollection.observable.Observable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*


class ServiceDownloadUtil<E> {
    private val mutex = Mutex()

    private val elements = LinkedList<E>()
    val size = Observable(0)

    var downloaded = 0
    var totalSize = 0

    suspend operator fun plusAssign(downloads: List<E>) {
        mutex.withLock {
            totalSize += downloads.size
            size.value += downloads.size
            elements += downloads
        }
    }

    suspend operator fun plusAssign(download: E) {
        mutex.withLock {
            totalSize++
            size.value++
            elements += download
        }
    }

    suspend fun clear() {
        mutex.withLock {
            downloaded = 0
            totalSize = 0
            size.value = 0
            elements.clear()
        }
    }

    suspend fun announceFinishedDownload() {
        mutex.withLock {
            if (size.value > 0) {
                size.value--
                downloaded++
            }
        }
    }

    suspend fun popOrNull(): E? {
        return mutex.withLock {
            elements.removeLastOrNull()
        }
    }
}