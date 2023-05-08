package de.yochyo.yummybooru.database

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.utils.enums.TagSortType

class PreferenceHelper(val context: Context) {
    val prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)

    var limit: Int
        get() = getPreference(context.getString(R.string.page_size), context.getString(R.string.page_size_default_value)).toInt()
        set(value) = setPreference(context.getString(R.string.page_size), value.toString())
    var saveTagsInTxt: Boolean
        get() = getPreference(context.getString(R.string.tags_in_txt), context.resources.getBoolean(R.bool.tags_in_txt_default_value))
        set(value) = setPreference(context.getString(R.string.tags_in_txt), value.toString())


    var selectedServerId: Int
        get() = getPreference(context.getString(R.string.currentServer), 1)
        set(value) = setPreference(context.getString(R.string.currentServer), value)

    var lastVersion: Int
        get() = getPreference(context.getString(R.string.lastVersion), BuildConfig.VERSION_CODE)
        set(value) = setPreference(context.getString(R.string.lastVersion), value)

    var downloadOriginal: Boolean
        get() = getPreference(context.getString(R.string.downloadOriginal), context.resources.getBoolean(R.bool.downloadOriginal_default_value))
        set(value) = setPreference(context.getString(R.string.downloadOriginal), value)

    var downloadWebm: Boolean
        get() = getPreference(context.getString(R.string.downloadWebm), context.resources.getBoolean(R.bool.downloadWebm_default_value))
        set(value) = setPreference(context.getString(R.string.downloadWebm), value)
    var preloadedImages: Int
        get() = getPreference(context.getString(R.string.preloaded_large_pictures), context.resources.getInteger(R.integer.preloaded_large_pictures_default_value))
        set(value) = setPreference(context.getString(R.string.preloaded_large_pictures), value)
    var clickToMoveToNextPicture: Boolean
        get() = getPreference(context.getString(R.string.click_to_move_to_next_image), context.resources.getBoolean(R.bool.click_to_move_to_next_image_default_value))
        set(value) = setPreference(context.getString(R.string.click_to_move_to_next_image), value)
    var parallelBackgroundDownloads: Int
        get() = getPreference(context.getString(R.string.parallel_background_downloads), context.resources.getInteger(R.integer.parallel_background_downloads_default_value))
        set(value) = setPreference(context.getString(R.string.parallel_background_downloads), value)
    var previewColumns: Int
        get() = getPreference(context.getString(R.string.preview_columns), context.resources.getInteger(R.integer.preview_columns_default_value))
        set(value) = setPreference(context.getString(R.string.preview_columns), value)
    var previewStaggeredMode: Boolean
        get() = getPreference(context.getString(R.string.preview_staggered_mode), context.resources.getBoolean(R.bool.preview_staggered_mode_default_value))
        set(value) = setPreference(context.getString(R.string.preview_staggered_mode), value)
    var cropPreviewImage: Boolean
        get() = getPreference(context.getString(R.string.crop_preview), context.resources.getBoolean(R.bool.crop_preview_default_value))
        set(value) = setPreference(context.getString(R.string.crop_preview), value)

    var saveFolderUri: String
        get() = getPreference(context.getString(R.string.savePath), "")
        set(value) = setPreference(context.getString(R.string.savePath), value)
    var saveFolder: DocumentFile
        get() = DocumentFile.fromTreeUri(context, Uri.parse(saveFolderUri))!!
        set(value) {
            saveFolderUri = value.uri.toString()
        }

    var isFirstStart: Boolean
        get() = getPreference(context.getString(R.string.is_first_app_usage), context.resources.getBoolean(R.bool.is_first_app_usage_default_value))
        set(value) = setPreference(context.getString(R.string.is_first_app_usage), value)
    var useNomedia: Boolean
        get() = getPreference(context.getString(R.string.use_nomedia), context.resources.getBoolean(R.bool.use_nomedia_default_value))
        set(value) = setPreference(context.getString(R.string.use_nomedia), value)
    var hideDownloadToast: Boolean
        get() = getPreference(context.getString(R.string.hide_download_toast), context.resources.getBoolean(R.bool.hide_download_toast_default_value))
        set(value) = setPreference(context.getString(R.string.hide_download_toast), value)
    var combinedSearchSort: Int
        get() = getPreference(context.getString(R.string.combined_search_sort), context.resources.getInteger(R.integer.combined_search_sort_default_value))
        set(value) = setPreference(context.getString(R.string.combined_search_sort), value)
    var sortTagsByFavoriteFirst: Boolean
        get() = getPreference(context.getString(R.string.show_favorites_first), context.resources.getBoolean(R.bool.show_favorites_first_default_value))
        set(value) = setPreference(context.getString(R.string.show_favorites_first), value)
    var sortTagsByTagType: Boolean
        get() = getPreference(context.getString(R.string.sort_by_tag_type), context.resources.getBoolean(R.bool.sort_by_tag_type_default_value))
        set(value) = setPreference(context.getString(R.string.sort_by_tag_type), value)
    var tagSortType: TagSortType
        get() = TagSortType.fromValue(
            getPreference(
                context.getString(R.string.sort_tag_comparator),
                context.resources.getString(R.string.sort_tag_comparator_default_value)
            ).toInt()
        )
        set(value) = setPreference(context.getString(R.string.sort_tag_comparator), value.value.toString())

    var enableTagCollectionMode: Boolean
        get() = getPreference(context.getString(R.string.tag_collection_mode), context.resources.getBoolean(R.bool.tag_collection_mode_default_value))
        set(value) = setPreference(context.getString(R.string.tag_collection_mode), value)

    var enableWindowPrivacy: Boolean
        get() = getPreference(context.getString(R.string.window_privacy), context.resources.getBoolean(R.bool.window_privacy_default_value))
        set(value) = setPreference(context.getString(R.string.window_privacy), value)

    private fun getPreference(name: String, default: String) = prefs.getString(name, default)!!
    private fun getPreference(name: String, default: Int = 0) = prefs.getInt(name, default)
    private fun getPreference(name: String, default: Boolean = false) = prefs.getBoolean(name, default)
    fun setPreference(name: String, value: String) = with(prefs.edit()) {
        putString(name, value)
        apply()
    }

    fun setPreference(name: String, value: Int) = with(prefs.edit()) {
        putInt(name, value)
        apply()
    }

    fun setPreference(name: String, value: Boolean) = with(prefs.edit()) {
        putBoolean(name, value)
        apply()
    }
}

private var _preferences: PreferenceHelper? = null
val Context.preferences: PreferenceHelper
    get() {
        return _preferences ?: PreferenceHelper((this)).apply { _preferences = this }
    }

