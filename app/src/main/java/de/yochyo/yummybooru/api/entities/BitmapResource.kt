package de.yochyo.yummybooru.api.entities

import android.graphics.Bitmap
import de.yochyo.yummybooru.utils.general.toBitmap
import java.io.InputStream

class BitmapResource(input: InputStream) : Resource2("png", input) {
    val bitmap: Bitmap = input.readBytes().toBitmap()!!

    companion object {
        fun from(resource2: Resource2): BitmapResource {
            return BitmapResource(resource2.input)
            resource2.input.close()
        }
    }

}