package de.yochyo.yummybooru.layout.views.mediaview.mediaViewImpl

import android.content.Context
import android.graphics.Point
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface
import de.yochyo.mediaview.mediaViewImpl.MediaViewImpl

class VideoImpl(private val context: Context, val surface: Surface) : MediaViewImpl {
    override var onSizeChange = { width: Int, height: Int -> }
    override fun destroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
    }

    override fun setUri(uri: Uri, headers: Map<String, String>?) {
        this.uri = uri
        this.headers = headers
        openVideo()
    }

    /*
    override fun onChangeScale(scale: Double, position: Point) {

    }
     */

    private var uri: Uri? = null
    private var headers: Map<String, String>? = null

    private var mMediaPlayer: MediaPlayer? = null

    private var requestAudioFocus = true


    fun openVideo() {
        if (uri == null)
            return
        destroy()
        try {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer!!.setOnPreparedListener { mMediaPlayer?.start() }


            mMediaPlayer!!.setOnVideoSizeChangedListener { _, width, height -> onSizeChange(width, height) }
            mMediaPlayer!!.isLooping = true
            mMediaPlayer!!.setDataSource(context.applicationContext, uri, headers)
            mMediaPlayer!!.setSurface(surface)
            mMediaPlayer!!.prepareAsync()
        } catch (e: Exception) {
            Log.w("MediaViewVideoImpl", "Unable to open content: $uri", e)
        }
    }
}