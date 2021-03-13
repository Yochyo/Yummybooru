package de.yochyo.yummybooru.utils.general

import android.content.Context
import android.view.Window
import android.view.WindowManager
import de.yochyo.yummybooru.database.preferences

object Configuration {
    fun setWindowSecurityFrag(context: Context, window: Window) {
        if (context.preferences.enableWindowPrivacy)
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }
}