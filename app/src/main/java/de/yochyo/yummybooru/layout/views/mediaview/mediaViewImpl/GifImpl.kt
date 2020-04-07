package de.yochyo.mediaview.mediaViewImpl

import android.graphics.Movie
import android.net.Uri
import de.yochyo.yummybooru.layout.views.mediaview.MediaView
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import java.io.FilterInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class GifImpl(val view: MediaView) : MediaViewImpl {
    private var job: Job? = null
    private var movie: Movie? = null
    private var gifStart = 0L
    override var onSizeChange = { _: Int, _: Int -> }

    override fun destroy() {
        runBlocking { job?.cancelAndJoin() }
    }

    override fun setUri(uri: Uri, headers: Map<String, String>?) {
        job = GlobalScope.launch {
            val input = getInputStreamFromUri(uri)
            movie = Movie.decodeStream(input)

            withContext(Dispatchers.Main) {
                onSizeChange(movie!!.width(), movie!!.height())

                while (isActive) {
                    delay(50)


                    if (gifStart == 0L) {
                        gifStart = System.currentTimeMillis()
                    }
                    var duration = movie!!.duration()
                    if (duration == 0) {
                        duration = 1000
                    }
                    val movieWidth = movie!!.width()
                    val movieHeight = movie!!.height()

                    val width = view.getWidth().toFloat()
                    val height = view.getHeight().toFloat()
                    val relTime = ((System.currentTimeMillis() - gifStart) % duration).toInt()
                    movie!!.setTime(relTime)

                    val canvas = view.lockCanvas()
                    canvas.scale(width / movieWidth, height / movieHeight);
                    movie!!.draw(canvas, 0f, 0f)
                    view.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    private suspend fun getInputStreamFromUri(uri: Uri): InputStream? {
        return withContext(Dispatchers.IO) {
            try {
                view.context.contentResolver.openInputStream(uri)
            } catch (e: FileNotFoundException) { //download from url
                try {
                    val url = URL(uri.toString())
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connect()
                    connection.inputStream
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