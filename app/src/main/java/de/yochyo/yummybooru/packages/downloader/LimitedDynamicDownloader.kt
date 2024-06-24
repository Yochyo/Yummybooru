package de.yochyo.downloader

import kotlin.math.min

/**
 * A dynamic downloader with a maximum of amountOfDownloads/proportion or @property maxCoroutines parallel downloads
 */
abstract class LimitedDynamicDownloader<E>(proportion: Int = 2, maxCoroutines: Int) : DynamicDownloader<E>(proportion) {
    init {
        this.maxCoroutines = maxCoroutines
    }

    override fun updateJobAmount() {
        while (downloads.size > activeCoroutines * proportion && activeCoroutines < min(maxCoroutines, downloads.size))
            startCoroutine()
    }
}