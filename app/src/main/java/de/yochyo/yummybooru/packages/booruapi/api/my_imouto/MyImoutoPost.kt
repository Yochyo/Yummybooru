package de.yochyo.booruapi.api.my_imouto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.api.Tag
import de.yochyo.booruapi.deserializers.LongDateDeserializer
import de.yochyo.booruapi.utils.extension
import kotlinx.coroutines.runBlocking
import java.util.Date

//TODO comments
data class MyImoutoPost(
    override val id: Int,

    @JsonProperty("tags") override val tagString: String,

    val fileUrl: String,
    val jpegUrl: String,
    val sampleUrl: String,
    val previewUrl: String,

    override val width: Int,
    override val height: Int,
    val sampleWidth: Int,
    val sampleHeight: Int,
    val jpegWidth: Int,
    val jpegHeight: Int,
    val previewWidth: Int,
    val previewHeight: Int,
    val actualPreviewWidth: Int,
    val actualPreviewHeight: Int,

    @JsonDeserialize(using = LongDateDeserializer::class) val createdAt: Date,
    val creatorId: Int,
    val author: String,
    val change: Int,
    val source: String,
    val score: Int,
    val md5: String,
    override val fileSize: Int,
    val isShownInIndex: Boolean,
    val sampleFileSize: Int,
    val jpegFileSize: Int,
    override val rating: String,
    val hasChildren: Boolean,
    val parentId: Int?,
    val status: String,
    val isHeld: Boolean,
    //TODO val framesPendingString: String,
    //TODO val framesPending: String,
    //TODO val framesString: String,
    //TODO val frames: String,
    var myImoutoApi: MyImoutoApi? = null,
) : Post(id, fileUrl.extension(), width, height, rating, fileSize, fileUrl, sampleUrl, previewUrl, tagString) {

    private val _tags: List<Tag> by lazy {
        val finalApi = myImoutoApi
        if (finalApi == null) super.getTags()
        else runBlocking { MyImoutoUtils.parseTagsFromUrl(finalApi.host, id) ?: super.getTags() }
    }

    /**
     * @return this method will return default values if no api was passed in this classes contructor.
     */
    @JsonIgnore
    override fun getTags(): List<Tag> = _tags
}