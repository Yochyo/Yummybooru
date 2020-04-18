package de.yochyo.yummybooru.api.entities

import android.widget.ImageView
import com.bumptech.glide.Glide
import de.yochyo.yummybooru.utils.general.Logger
import de.yochyo.yummybooru.utils.general.toBitmap
import de.yochyo.yummybooru.utils.general.tryCatch
import java.io.*

class Resource(val resource: ByteArray, val mimetype: String) : Serializable {
    val type = typeFromMimeType(mimetype)


    companion object {
        const val IMAGE = 0
        const val ANIMATED = 1
        const val VIDEO = 2

        fun typeFromMimeType(mimetype: String) = when (mimetype) {
            "png", "jpg" -> IMAGE
            "gif" -> ANIMATED
            "mp4", "webm" -> VIDEO
            else -> IMAGE
        }

        fun fromFile(file: File): Resource? {
            try {
                val fileStream = file.inputStream()
                val inputStream = ObjectInputStream(fileStream)
                val obj = inputStream.readObject() as Resource
                fileStream.close()
                inputStream.close()
                return obj
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.log(e, file.name)
            }
            return null
        }
    }

    fun loadIntoImageView(imageView: ImageView) {
        when (type) {
            IMAGE -> imageView.setImageBitmap(resource.toBitmap())
            ANIMATED -> tryCatch { Glide.with(imageView).load(resource).into(imageView) }
        }
    }

    fun loadIntoImageView(file: File) {
        val outputStream = ByteArrayOutputStream()
        val objStream = ObjectOutputStream(outputStream)
        try {
            objStream.writeObject(this)
            file.writeBytes(outputStream.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.log(e, file.name)
        } finally {
            objStream.close()
            outputStream.close()
        }
    }
}
