package de.yochyo.downloader

/**
 * A dynamic downloader with a maximum of amountOfDownloads/proportion parallel downloads
 */
abstract class DynamicDownloader<E>(var proportion: Int = 2) : RegulatingDownloader<E>(Integer.MAX_VALUE) {
    override fun updateJobAmount() {
        while (downloads.size > activeCoroutines * proportion)
            startCoroutine()
    }
}