package de.yochyo.yummybooru.layout.views.mediaview.mediaViewImpl

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.Surface
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import de.yochyo.mediaview.mediaViewImpl.MediaViewImpl
import de.yochyo.yummybooru.R


class VideoImpl(private val context: Context, val surface: Surface) : MediaViewImpl {
    override var onSizeChange = { width: Int, height: Int -> }

    private var uri: Uri? = null
    private var headers: Map<String, String>? = null

    private var mPlayer: SimpleExoPlayer? = null

    override fun destroy() {
        mPlayer?.release()
        mPlayer = null
    }

    override fun pause() {
        mPlayer?.playWhenReady = false
    }

    override fun resume() {
        mPlayer?.playWhenReady = true
    }

    override fun setUri(uri: Uri, headers: Map<String, String>?) {
        this.uri = uri
        this.headers = headers
        openVideo()
    }

    fun openVideo() {
        if (uri == null)
            return
        destroy()
        try {
            mPlayer = SimpleExoPlayer.Builder(context).build()
            mPlayer?.repeatMode = SimpleExoPlayer.REPEAT_MODE_ONE
            mPlayer?.setVideoSurface(surface)
            mPlayer?.playWhenReady = true
            mPlayer?.addVideoListener(object: VideoListener{
                override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                    onSizeChange(width, height)
                }
            })
            val factory = DefaultDataSourceFactory(context, context.getString(R.string.app_name))
            val source = ProgressiveMediaSource.Factory(factory).createMediaSource(uri!!)
            mPlayer?.prepare(source)

        } catch (e: Exception) {
            Log.w("MediaViewVideoImpl", "Unable to open content: $uri", e)
        }
    }
}