package de.yochyo.yummybooru.layout.views.mediaview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.GLES20
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.View
import de.yochyo.mediaview.mediaViewImpl.MediaViewImpl
import de.yochyo.yummybooru.layout.views.mediaview.mediaViewImpl.VideoImpl
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import kotlin.math.roundToInt

//TODO you cannot change the media type after drawing a bitmap. (https://source.android.com/devices/graphics/arch-sh.html#canvas)
class MediaView(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
        TextureView(context, attrs, defStyle) {

    private var paused = true
    //TODO ACCEPT Inputstream/ByteArray
    private var onAvailable: () -> Unit = { }
    private var mSurface: Surface? = null

    var impl: MediaViewImpl? = null

    private var mediaWidth = 0
    private var mediaHeight = 0

    private val mSurfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

        @SuppressLint("Recycle")
        override fun onSurfaceTextureAvailable(s: SurfaceTexture, width: Int, height: Int) {
            mSurface = Surface(s)
            onAvailable()
        }

        override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean = destroy()

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private companion object {
        const val VIDEO = 0
    }

    init {
        surfaceTextureListener = mSurfaceTextureListener
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }


    fun setVideoPath(path: String, headers: Map<String, String>) = setVideoUri(Uri.parse(path), headers)
    fun setVideoUri(uri: Uri, headers: Map<String, String>) = setImpl(VIDEO, uri, headers)

    private fun setImpl(type: Int, uri: Uri, headers: Map<String, String>) {
        val f: () -> Unit = {
            impl?.destroy()

            clearSurface()
            impl = VideoImpl(context, mSurface!!, paused)


            impl?.onSizeChange = { width, height -> setSize(width, height) }
            impl?.setUri(uri, headers)
        }

        if (isAvailable) f()
        else onAvailable = f
    }

    private fun setSize(width: Int, height: Int) {
        surfaceTexture?.setDefaultBufferSize(width, height)
        mediaWidth = width
        mediaHeight = height
        requestLayout()
    }
    fun resume(){
        impl?.resume()
        paused = false
    }
    fun pause(){
        impl?.pause()
        paused = true
    }

    fun destroy(): Boolean {
        impl?.destroy()
        impl = null
        clearSurface()
        surfaceTexture?.release()
        mSurface?.release()
        mSurface = null
        return true
    }


    /**
     * Clears the surface texture by attaching a GL context and clearing it.
     * Code taken from [Hugo Gresse's answer on stackoverflow.com](http://stackoverflow.com/a/31582209).
     */
    private fun clearSurface() {
        if (mSurface == null)
            return

        val egl = EGLContext.getEGL() as EGL10
        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        egl.eglInitialize(display, null)
        val attribList = intArrayOf(
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, EGL10.EGL_WINDOW_BIT,
                EGL10.EGL_NONE, 0,  // placeholder for recordable [@-3]
                EGL10.EGL_NONE
        )
        val configs =
                arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        egl.eglChooseConfig(display, attribList, configs, configs.size, numConfigs)
        val config = configs[0]
        val context = egl.eglCreateContext(
                display, config, EGL10.EGL_NO_CONTEXT, intArrayOf(12440, 2, EGL10.EGL_NONE)
        )
        val eglSurface = egl.eglCreateWindowSurface(display, config, mSurface, intArrayOf(EGL10.EGL_NONE))
        egl.eglMakeCurrent(display, eglSurface, eglSurface, context)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        egl.eglSwapBuffers(display, eglSurface)
        egl.eglDestroySurface(display, eglSurface)
        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
        egl.eglDestroyContext(display, context)
        egl.eglTerminate(display)
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = View.getDefaultSize(mediaWidth, widthMeasureSpec)
        var height = View.getDefaultSize(mediaHeight, heightMeasureSpec)

        if (mediaWidth > 0 && mediaHeight > 0) {
            val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
            val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
            val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                width = widthSpecSize
                height = heightSpecSize


                // for compatibility, we adjust size based on aspect ratio
                if (mediaWidth * height < width * mediaHeight) {
                    width = height * mediaWidth / mediaHeight
                } else if (mediaWidth * height > width * mediaHeight) {
                    height = width * mediaHeight / mediaWidth
                }

            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                width = widthSpecSize
                height = width * mediaHeight / mediaWidth
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize)
                    height = heightSpecSize

            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                height = heightSpecSize
                width = height * mediaWidth / mediaHeight
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize)
                    width = widthSpecSize
            } else {
                //use actual video size
                if (mediaWidth < mediaHeight) {
                    val widthFactor = widthSpecSize.toDouble() / mediaWidth
                    width = (mediaWidth * widthFactor).roundToInt()
                    height = (mediaHeight * widthFactor).roundToInt()

                } else {
                    val heightFactor = heightSpecSize.toDouble() / mediaHeight
                    height = (mediaHeight * heightFactor).roundToInt()
                    width = (mediaWidth * heightFactor).roundToInt()
                }
                if (width > widthSpecSize) {
                    val toLargeFactor = width.toDouble() / widthSpecSize
                    width = widthSpecSize
                    height = (height.toDouble() / toLargeFactor).roundToInt()
                }
                if (height > heightSpecSize) {
                    val toLargeFactor = height.toDouble() / heightSpecSize
                    height = heightSpecSize
                    width = (width.toDouble() / toLargeFactor).roundToInt()
                }

            }
        }
        setMeasuredDimension(width, height)
    }
}

