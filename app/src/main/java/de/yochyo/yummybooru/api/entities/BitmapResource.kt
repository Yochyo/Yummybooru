package de.yochyo.yummybooru.api.entities

import android.graphics.Bitmap
import de.yochyo.yummybooru.utils.general.toBitmap
import java.io.InputStream

class BitmapResource(input: InputStream) : Resource2("png", input) {
    val bitmap: Bitmap = input.toBitmap()!!

    @Deprecated("InputStream is already closed, use bitmap")
    override val input: InputStream = input

    init {
        input.close()
    }

    companion object {
        fun from(resource2: Resource2?): BitmapResource? {
            return if (resource2 == null) null
            else BitmapResource(resource2.input)
        }
    }

}