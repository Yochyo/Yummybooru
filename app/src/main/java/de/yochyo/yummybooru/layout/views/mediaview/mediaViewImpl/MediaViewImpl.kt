package de.yochyo.mediaview.mediaViewImpl

import android.graphics.Point
import android.net.Uri
import java.net.URL

interface MediaViewImpl{
    var onSizeChange: (Int, Int) -> Unit
    fun setUri(uri: Uri, headers: Map<String, String>? = null)
  //  fun onChangeScale(scale: Double, position: Point)
    fun destroy()

    fun pause()
    fun resume()
}