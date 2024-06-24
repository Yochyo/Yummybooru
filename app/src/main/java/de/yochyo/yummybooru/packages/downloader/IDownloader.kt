package de.yochyo.downloader


interface IDownloader<E> {
    fun download(url: String, callback: suspend (e: E?) -> Unit, headers: Map<String, String>, context: Any)
    fun downloadNow(url: String, callback: suspend (e: E?) -> Unit, headers: Map<String, String>, context: Any)
    suspend fun downloadSync(url: String, headers: Map<String, String>, context: Any): E?
    fun stop()
}