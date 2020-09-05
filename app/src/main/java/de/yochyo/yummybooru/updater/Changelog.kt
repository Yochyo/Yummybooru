package de.yochyo.yummybooru.updater

import android.content.Context
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.ShowChangelogsDialog

class Changelog(val versionName: String, val version: Int, val description: String) {
    companion object {
        private val logs = ArrayList<Changelog>()

        init {
            logs += Changelog("1.0", 0, "- First Version")
            logs += Changelog("1.1", 1, "- Bug fixes")
            logs += Changelog("1.3", 2, "- You can now download all pictures of a type automatically\n" +
                    "- application got faster\n" +
                    "- some bug fixes\n" +
                    "- preparation for future updates")
            logs += Changelog("1.3.1", 3, "- Some small bugfixes")
            logs += Changelog("1.4", 4, "- App can now be used starting Android 5.0 Lollipop\n" +
                    "- Fixed a lot of small bugs (most of them did not impact the user experience)\n" +
                    "- Editing/Favoriting/... tags or subs is now more convenient than ever\n" +
                    "- You can now multiselect pictures and download them all.\n" +
                    "- Some visual changes\n" +
                    "- Optimized scrolling experience\n" +
                    "- Double swipe up a picture to add the artists to the tag history")
            logs += Changelog("1.5", 5, "- You can now filter tags in the startscreen (Thanks to Water_LP for suggesting it in Discord)\n" +
                    "- You can now update several Subscriptions directly and more comfortable\n" +
                    "- App will now update automatically when an new version is out (Yay)")
            logs += Changelog(
                "2.0", 6, "- You can now see the changelog in the settings\n" +
                        "- Gifs can now be displayed and downloaded\n" +
                        "- Reduced app size\n" +
                        "- You can now add/favorite/subscribe a tag while scrolling through the images\n" +
                        "- You can now loose your internet connection for several hours and continue using the app without errors\n" +
                        "- Tags for Moebooru Clients are now displayed in the right order\n" +
                        "- Backups were overworked (please create new backups, old ones will no longer work)\n" +
                        "- You can now search for tags with special characters (for example 6+girls)\n" +
                        "- Add an auto mode when adding a server\n" +
                        "- You can now add special tags (width/height of image)\n" +
                        "- You can now search for Subscriptions\n" +
                        "- You can now deselect all selected tags on the startscreen\n" +
                        "- The current amount of new images (Subscriptions) is now better displayed\n" +
                        "- Fixed the bug where the app crashed while scrolling through images\n" +
                        "- A lot of bug fixes and small changes\n" +
                        "- New icon"
            )
            logs += Changelog("2.01", 7, "- Small bug fixes")
            logs += Changelog("2.2", 9, "Added gelbooru Support\n" +
                    "Can now display videos\n" +
                    "Fixed some bugs\n" +
                    "Can now login into danbooru again\n" +
                    "Added 'Download All' feature\n" +
                    "...")
        }

        fun showChangelogs(context: Context) {
            ShowChangelogsDialog().withChangelogs(changeLogs().reversed()).build(context)
        }

        fun showChangelogIfChanges(context: Context) {
            if (BuildConfig.VERSION_CODE != context.db.lastVersion) {
                showChangelogs(context)
            }
            context.db.lastVersion = BuildConfig.VERSION_CODE
        }

        fun changeLogs(): List<Changelog> = logs
    }
}