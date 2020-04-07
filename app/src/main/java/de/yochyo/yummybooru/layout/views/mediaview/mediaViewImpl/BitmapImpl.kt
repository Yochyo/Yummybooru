package de.yochyo.mediaview.mediaViewImpl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import de.yochyo.yummybooru.layout.views.mediaview.MediaView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

class BitmapImpl(val view: MediaView) : MediaViewImpl {
    override var onSizeChange = { width: Int, height: Int -> }


    override fun setUri(uri: Uri, headers: Map<String, String>?) {
        GlobalScope.launch {
            val bitmap = getBitmapFromUri(uri)
            if (bitmap != null)
                withContext(Dispatchers.Main) { initBitmap(bitmap) }
        }
    }

    override fun destroy() {

    }

    private fun initBitmap(bitmap: Bitmap) {
        onSizeChange(bitmap.width, bitmap.height)

        try {
            val canvas = view.lockCanvas()
            canvas.drawBitmap(bitmap, Matrix(), Paint())
            view.unlockCanvasAndPost(canvas)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private suspend fun getBitmapFromUri(uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val input = view.context.contentResolver.openInputStream(uri)!!
                val bitmap = BitmapFactory.decodeStream(input)
                input.close()
                bitmap
            } catch (e: FileNotFoundException) { //download from url
                try {
                    val url = URL(uri.toString())
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connect()
                    val input = connection.getInputStream()
                    val bitmap = BitmapFactory.decodeStream(input)
                    input.close()
                    bitmap
                } catch (e: Exception) {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

}