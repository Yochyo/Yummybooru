package de.yochyo.yummybooru.updater

import android.content.Context
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.ShowChangelogsDialog

class Changelog(val versionName: String, val version: Int, val description: String) {
    companion object{
        private val logs = ArrayList<Changelog>()
        init{
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
        }

        fun showChangelogs(context: Context){
            if (BuildConfig.VERSION_CODE != db.lastVersion) {
                db.lastVersion = BuildConfig.VERSION_CODE
                ShowChangelogsDialog().withChangelogs(Changelog.changeLogs().reversed()).build(context)
            }
        }

        fun changeLogs(): List<Changelog> = changeLogs(0, BuildConfig.VERSION_CODE)
        fun changeLogs(first: Int, last: Int): List<Changelog> = logs.subList(first, last)
    }
}