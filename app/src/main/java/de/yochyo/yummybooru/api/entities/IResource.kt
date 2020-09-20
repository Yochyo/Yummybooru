package de.yochyo.yummybooru.api.entities

interface IResource {
    val mimetype: String
    val type get() = typeFromMimeType(mimetype)


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
    }
}