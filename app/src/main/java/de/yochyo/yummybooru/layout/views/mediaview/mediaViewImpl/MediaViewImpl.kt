package de.yochyo.mediaview.mediaViewImpl

import android.net.Uri

interface MediaViewImpl{
    var onSizeChange: (Int, Int) -> Unit
    fun setUri(uri: Uri, headers: Map<String, String> = emptyMap())

    //  fun onChangeScale(scale: Double, position: Point)
    fun destroy()

    fun pause()
    fun resume()
}