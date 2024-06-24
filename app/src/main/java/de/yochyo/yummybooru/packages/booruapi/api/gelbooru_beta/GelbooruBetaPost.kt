package de.yochyo.booruapi.api.gelbooru_beta

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.api.Tag
import de.yochyo.booruapi.utils.extension
import kotlinx.coroutines.runBlocking
import java.net.URL

data class GelbooruBetaPost(
    override val id: Int,

    val fileUrl: String,
    @JsonProperty("sample_url") private val _fileSampleURL: String,
    @JsonProperty("preview_url") override val filePreviewURL: String,
    @JsonProperty("tags") override val tagString: String,

    override val height: Int,
    override val width: Int,
    val previewWidth: Int,
    val previewHeight: Int,

    val score: Int,
    val parentId: Int,
    override val rating: String,
    val change: Long,
    val md5: String,
    val creatorId: Int,
    val hasChildren: Boolean,
//    todo commented out cause date conversion throws errors val createdAt: Date,
    val status: String,
    val source: String,
    val hasNotes: Boolean,
    val hasComments: Boolean,

    var gelbooruBetaApi: GelbooruBetaApi? = null,
) : Post(id, fileUrl.extension(), width, height, rating, 0, fileUrl, getSampleUrl(_fileSampleURL), filePreviewURL, tagString) {

    companion object {
        /**
         * When a post has a sample (most of the time, the sampleUrl is the same as the fileUrl cause there isn't a sample),
         * the url has an error.
         * https://rule34.xxx/samples/xxx redirects to the html page for post xxx
         * https://rule34.xxx//samples/xxx would be the correct url for the sample
         */
        private fun getSampleUrl(wrongSampleUrl: String): String {
            val host = URL(wrongSampleUrl).host
            return "https://$host/${wrongSampleUrl.substringAfter(host)}"
        }
    }

    private val _tags: List<Tag> by lazy {
        val finalApi = gelbooruBetaApi
        if (finalApi == null) super.getTags()
        else runBlocking { GelbooruBetaUtils.parseTagsFromUrl(finalApi.host, id) ?: super.getTags() }
    }

    /**
     * @return this method will return default values if no api was passed in this classes contructor.
     */
    @JsonIgnore
    override fun getTags(): List<Tag> = _tags
}