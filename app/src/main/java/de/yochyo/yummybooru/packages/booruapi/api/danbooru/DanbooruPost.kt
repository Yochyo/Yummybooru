package danbooru

import com.fasterxml.jackson.annotation.JsonProperty
import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.api.Tag
import de.yochyo.booruapi.api.danbooru.DanbooruTag
import de.yochyo.booruapi.utils.extension
import java.util.Date

//TODO comments
data class DanbooruPost(
    override val id: Int,

    override val tagString: String,
    val tagStringGeneral: String,
    val tagStringCharacter: String,
    val tagStringCopyright: String,
    val tagStringArtist: String,
    val tagStringMeta: String,
    val tagCount: Int,
    val tagCountGeneral: Int,
    val tagCountArtist: Int,
    val tagCountCharacter: Int,
    val tagCountCopyright: Int,


    val fileUrl: String = "",
    val previewFileUrl: String = "",
    val largeFileUrl: String = "",

    @JsonProperty("image_width") override val width: Int,
    @JsonProperty("image_height") override val height: Int,

    override val rating: String,
    val score: Int,
    val md5: String?,
    val createdAt: Date,
    val source: String,
    @JsonProperty("file_ext") val fileExtension: String?,

    val lastCommentBumpedAt: Date?,
    val isNoteLocked: Boolean,
    val favCount: Boolean,

    val parentId: Int?,
    val hasChildren: Boolean,

    val lastNotedAt: Date?,
    val isRatingLocked: Boolean,
    val approverId: Int?,
    val uploaderId: Int,
    override val fileSize: Int,
    val isStatusLocked: Boolean,
    val poolString: String?,
    val upScore: Int,
    val downScore: Int,
    val isPending: Boolean,
    val isFlagged: Boolean,
    val isDeleted: Boolean,
    val updatedAt: Date,
    val isBanned: Boolean,
    val pixivId: Int?,
    val lastCommentedAt: Date?,
    val hasActiveChildren: Boolean,
    val bitFlags: Int,
    val tagCountMeta: Int,
    val hasLarge: Boolean,
    val hasVisibleChildren: Boolean,
    val isFavorited: Boolean,
) : Post(
    id, fileExtension
        ?: fileUrl.extension(), width, height, rating, fileSize, fileUrl, largeFileUrl, previewFileUrl, tagString
) {
    private val _tags by lazy {
        ArrayList<Tag>().apply {
            val tagsGeneral = tagStringGeneral.split(" ").filter { it != "" }.map { Tag(it, DanbooruTag.typeToTypeEnum(DanbooruTag.DANBOORU_GENERAL), 0) }
            val tagsCharacter = tagStringCharacter.split(" ").filter { it != "" }.map { Tag(it, DanbooruTag.typeToTypeEnum(DanbooruTag.DANBOORU_CHARACTER), 0) }
            val tagsCopyright = tagStringCopyright.split(" ").filter { it != "" }.map { Tag(it, DanbooruTag.typeToTypeEnum(DanbooruTag.DANBOORU_COPYRIGHT), 0) }
            val tagsArtist = tagStringArtist.split(" ").filter { it != "" }.map { Tag(it, DanbooruTag.typeToTypeEnum(DanbooruTag.DANBOORU_ARTIST), 0) }
            val tagsMeta = tagStringMeta.split(" ").filter { it != "" }.map { Tag(it, DanbooruTag.typeToTypeEnum(DanbooruTag.DANBOORU_META), 0) }
            this.addAll(tagsArtist)
            this.addAll(tagsCopyright)
            this.addAll(tagsCharacter)
            this.addAll(tagsGeneral)
            this.addAll(tagsMeta)
        }
    }

    override fun getTags(): List<Tag> = _tags
}


