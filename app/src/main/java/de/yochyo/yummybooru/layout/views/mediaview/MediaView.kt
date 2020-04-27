package de.yochyo.yummybooru.layout.views.mediaview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.GLES20
import android.util.AttributeSet
import android.util.Log
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

    //TODO ACCEPT Inputstream/ByteArray
    private var onAvailable: () -> Unit = { }
    private var mSurface: Surface? = null

    var impl: MediaViewImpl? = null

    private var mediaWidth = 0
    private var mediaHeight = 0


    /*
    var minScale = 1f
        set(value) {
            field =
                    if (value in 1.0f..maxScale) value else throw RuntimeException("minScale can't be lower than 1 or larger than maxScale($maxScale)")
        }
    var midScale = 1.6f
        set(value) {
            field =
                    if (value in minScale..mid2Scale) value else throw Exception("midScale ($value) is not in min..max ($minScale, $maxScale)")
        }
    var mid2Scale = 2f
        set(value) {
            field =
                    if (value in midScale..maxScale) value else throw RuntimeException("maxScale can't be lower than 1 or midScale($midScale)")
        }
    var maxScale = 5f
        set(value) {
            field =
                    if (value >= mid2Scale) value else throw RuntimeException("maxScale can't be lower than 1 or minScale($minScale)")
        }
     */

    private var currentScale = 1f

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
        const val BITMAP = 1
        const val GIF = 2


        const val SUPERSTATE_KEY = "superState"
        const val MIN_SCALE_KEY = "minScale"
        const val MAX_SCALE_KEY = "maxScale"
        const val DRAG = 1
        const val ZOOM = 2
    }

    init {
        surfaceTextureListener = mSurfaceTextureListener
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        //  initView(attrs)
    }

    /*
      private fun initView(attrs: AttributeSet?) {
          val a = context.theme.obtainStyledAttributes(attrs, R.styleable.ZoomableTextureView, 0, 0)
          try {
              minScale = a.getFloat(R.styleable.ZoomableTextureView_minScale, minScale)
              maxScale = a.getFloat(R.styleable.ZoomableTextureView_maxScale, maxScale)
          } finally {
              a.recycle()
          }
          setOnTouchListener(ZoomOnTouchListeners())
      }
     */

    fun setVideoPath(path: String, headers: Map<String, String>? = null) = setVideoUri(Uri.parse(path), headers)
    fun setVideoUri(uri: Uri, headers: Map<String, String>? = null) = setImpl(VIDEO, uri, headers)

    fun setGifPath(path: String, headers: Map<String, String>? = null) = setGifUri(Uri.parse(path), headers)
    fun setGifUri(uri: Uri, headers: Map<String, String>? = null) = setImpl(GIF, uri, headers)

    fun setBitmapPath(path: String, headers: Map<String, String>? = null) = setBitmapUri(Uri.parse(path), headers)
    fun setBitmapUri(uri: Uri, headers: Map<String, String>? = null) = setImpl(BITMAP, uri, headers)

    private fun setImpl(type: Int, uri: Uri, headers: Map<String, String>? = null) {
        val f: () -> Unit = {
            impl?.destroy()

            clearSurface()
            impl = VideoImpl(context, mSurface!!)


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
        println("SET SIZE: $width, $height")
        requestLayout()
    }

    fun destroy(): Boolean {
        impl?.destroy()
        impl = null
        clearSurface()
        surfaceTexture.release()
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


        Log.v("[View name] onMeasure w", MeasureSpec.toString(widthMeasureSpec))
        Log.v("[View name] onMeasure h", MeasureSpec.toString(heightMeasureSpec))

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

                    /*
                    if (width > widthSpecSize && widthSpecMode == MeasureSpec.AT_MOST) {
                        println("width to large")
                        // too wide, decrease both width and height
                        val toLargeFactor = width.toDouble() / widthSpecSize
                        width = widthSpecSize
                        height = (width.toDouble() / toLargeFactor).roundToInt()
                    }
                     */


                } else {
                    val heightFactor = heightSpecSize.toDouble() / mediaHeight
                    height = (mediaHeight * heightFactor).roundToInt()
                    width = (mediaWidth * heightFactor).roundToInt()
                    /*
                    if (height > heightSpecSize && heightSpecMode == MeasureSpec.AT_MOST) {
                        // too tall, decrease both width and height
                        val toLargeFactor = height.toDouble() / heightSpecSize
                        height = heightSpecSize
                        width = (width.toDouble() / toLargeFactor).roundToInt()
                    }
                     */
                }
                if (width > widthSpecSize) {
                    println("width to large")
                    val toLargeFactor = width.toDouble() / widthSpecSize
                    width = widthSpecSize
                    height = (height.toDouble() / toLargeFactor).roundToInt()
                    println("")
                }
                if (height > heightSpecSize) {
                    val toLargeFactor = height.toDouble() / heightSpecSize
                    height = heightSpecSize
                    width = (width.toDouble() / toLargeFactor).roundToInt()
                }

                println("cur width: $width, height: $height, mediawidth: $mediaWidth, mediaheight: $mediaHeight")

                /*
                if (mediaWidth < mediaHeight) {
                    println("width < height")
                    width = (height * ratio).roundToInt()
                    println("newwidth: $width")

                    if (width > widthSpecSize && widthSpecMode == MeasureSpec.AT_MOST) {
                        println("width to large")
                        // too wide, decrease both width and height
                        val toLargeFactor = width.toDouble() / widthSpecSize
                        width = widthSpecSize
                        height = (width.toDouble() / toLargeFactor).roundToInt()

                        }


                } else {
                    println("height < width")
                    height = (ratio / mediaHeight).pow(-1.0).roundToInt()
                    println("new height $height")
                    if (height > heightSpecSize && heightSpecMode == MeasureSpec.AT_MOST) {
                        // too tall, decrease both width and height
                        val toLargeFactor = height.toDouble() / heightSpecSize
                        height = heightSpecSize
                        width = (width.toDouble() / toLargeFactor).roundToInt()
                    }


                }
                 */


            }
        }
        setMeasuredDimension(width, height)
    }


    /*
    private inner class ZoomOnTouchListeners : OnTouchListener {
        private val mLast = PointF()

        private var mRight = 0f
        private var mBottom = 0f

        private var mMode = ZOOM
        val mMatrix = Matrix()
        private var mMatrixValues: FloatArray = FloatArray(9)


        private val mGestureDetector = GestureDetector(context, object : GestureDetector.OnGestureListener {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                return true
            }

            override fun onDown(e: MotionEvent?) = false
            override fun onLongPress(e: MotionEvent?) {}
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float) = false
            override fun onShowPress(e: MotionEvent?) {}
            override fun onSingleTapUp(e: MotionEvent?) = false
        })

        init {
            mGestureDetector.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if(currentScale == minScale) zoomTo(midScale, e.x, e.y)
                    else if(currentScale == midScale) zoomTo(mid2Scale, e.x, e.y)
                   // else zoomTo(1f, e.x, e.y)
                    //  requestLayout()
                    return true
                }

                override fun onDoubleTapEvent(e: MotionEvent?) = false
                override fun onSingleTapConfirmed(e: MotionEvent?) = false
            })
        }

        private val mScaleDetector =
            ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    mMode = ZOOM
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector?) {
                    mMode = DRAG
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    zoomBy(detector.scaleFactor, detector.focusX, detector.focusY)

                    mMatrix.getValues(mMatrixValues)
                    val x = mMatrixValues[Matrix.MTRANS_X]
                    val y = mMatrixValues[Matrix.MTRANS_Y]

                    if (y < -mBottom) mMatrix.postTranslate(0f, -(y + mBottom))
                    else if (y > 0) mMatrix.postTranslate(0f, -y)
                    if (x < -mRight) mMatrix.postTranslate(-(x + mRight), 0f)
                    else if (x > 0) mMatrix.postTranslate(-x, 0f)
                    return true
                }
            })

        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            mScaleDetector.onTouchEvent(motionEvent)
            mGestureDetector.onTouchEvent(motionEvent)
            mMatrix.getValues(mMatrixValues)
            val x = mMatrixValues[Matrix.MTRANS_X]
            val y = mMatrixValues[Matrix.MTRANS_Y]

            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mLast.set(motionEvent.x, motionEvent.y)
                    mMode = DRAG
                }
                MotionEvent.ACTION_UP -> mMode = DRAG
                MotionEvent.ACTION_POINTER_DOWN -> {
                    mLast.set(motionEvent.x, motionEvent.y)
                    mMode = DRAG
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mMode == DRAG) {
                        val curr = PointF(motionEvent.x, motionEvent.y)
                        var deltaX = curr.x - mLast.x
                        var deltaY = curr.y - mLast.y

                        if (y + deltaY > 0) deltaY = -y
                        else if (y + deltaY < -mBottom) deltaY = -(y + mBottom)

                        if (x + deltaX > 0) {
                            println("(${x+deltaX}, $x, $mRight)")
                            deltaX = -x
                        }
                        else if (x + deltaX < -mRight) {
                            println("(${x+deltaX}, ${-mRight})")
                            deltaX = -(x + mRight)
                        }

                        mMatrix.postTranslate(deltaX, deltaY)
                        mLast[curr.x] = curr.y
                    }
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    val index = if (motionEvent.actionIndex != 0) 0 else 1
                    mLast[motionEvent.getX(index)] = motionEvent.getY(index)
                    mMode = DRAG
                }
                MotionEvent.ACTION_CANCEL -> mMode = DRAG
            }
            setTransform(mMatrix)
            requestLayout()
            return true
        }

        fun zoomBy(scaleBy: Float, x: Float, y: Float) {
            var mScaleFactor = scaleBy
            val origScale = currentScale
            currentScale *= mScaleFactor
            if (currentScale > maxScale) {
                currentScale = maxScale
                mScaleFactor = maxScale / origScale
            } else if (currentScale < minScale) {
                currentScale = minScale
                mScaleFactor = minScale / origScale
            }

            zoom(mScaleFactor, x, y)
        }

        fun zoomTo(scaleTo: Float, x: Float, y: Float) {
            var mScaleFactor = scaleTo / currentScale
            val origScale = currentScale
            currentScale = scaleTo
            if (currentScale > maxScale) {
                currentScale = maxScale
                mScaleFactor = maxScale / origScale
            } else if (currentScale < minScale) {
                currentScale = minScale
                mScaleFactor = minScale / origScale
            }

            zoom(mScaleFactor, x, y)
        }

        private fun zoom(scale: Float, x: Float, y: Float) {
            println("[$mediaWidth, $currentScale, $mediaHeight]")
            mRight = mediaWidth * currentScale - mediaWidth
            mBottom = mediaHeight * currentScale - mediaHeight
            mMatrix.postScale(scale, scale, x, y)
        }
    }
     */

}

