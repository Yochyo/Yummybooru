package de.yochyo.downloader

import kotlinx.coroutines.CoroutineScope

@Deprecated("Use RegulatingDownloader instead")
abstract class Downloader<E>(coroutines: Int) : ADownloader<E>() {
    private var enabled = true

    init {
        for (i in 0 until coroutines)
            startCoroutine()
    }

    override fun keepCoroutineAliveWhile(scope: CoroutineScope) = enabled

    override fun onAddDownload() {
        enabled = true
    }

    override fun stop() {
        enabled = false
    }
}