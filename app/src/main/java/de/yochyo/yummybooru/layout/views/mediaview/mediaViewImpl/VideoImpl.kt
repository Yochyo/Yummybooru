package de.yochyo.mediaview.mediaViewImpl

import android.content.Context
import android.graphics.Point
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface

class VideoImpl(private val context: Context, val surface: Surface) : MediaViewImpl {
    override var onSizeChange = { width: Int, height: Int -> }
    override fun destroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
            mMediaPlayer = null
            if (requestAudioFocus) {
                val am = context.applicationContext
                    .getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.abandonAudioFocus(null)
            }
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
    private var audioSession: Int = 0


    fun openVideo() {
        if (uri == null)
            return

        destroy()
        if (requestAudioFocus) {
            val am = context.applicationContext
                .getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        try {
            mMediaPlayer = MediaPlayer()
            mMediaPlayer!!.setOnPreparedListener { mMediaPlayer?.start() }
            if (audioSession != 0) {
                mMediaPlayer!!.audioSessionId = audioSession
            } else {
                audioSession = mMediaPlayer!!.audioSessionId
            }


            mMediaPlayer!!.setOnVideoSizeChangedListener { _, width, height -> onSizeChange(width, height) }
            mMediaPlayer!!.isLooping = true
            /*
            mMediaPlayer!!.setOnPreparedListener(mPreparedListener)
            mMediaPlayer!!.setOnCompletionListener(mCompletionListener)
            mMediaPlayer!!.setOnErrorListener(mErrorListener)
            mMediaPlayer!!.setOnInfoListener(mInfoListener)
            mMediaPlayer!!.setOnBufferingUpdateListener(mBufferingUpdateListener)
             */
          //  mMediaPlayer!!.setDataSource(uri.toString())
            mMediaPlayer!!.setDataSource(context.applicationContext, uri, headers)
            mMediaPlayer!!.setSurface(surface)
            mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mMediaPlayer!!.prepareAsync()
        } catch (e: Exception) {
            Log.w("MediaViewVideoImpl", "Unable to open content: $uri", e)
        }
    }
}