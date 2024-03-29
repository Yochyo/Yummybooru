package de.yochyo.yummybooru.layout.activities.introactivities.introactivity

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro2
import com.github.appintro.R
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.layout.activities.introactivities.IIntroActivity
import de.yochyo.yummybooru.utils.general.Configuration

class IntroActivity : AppIntro2(), IIntroActivity {
    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.setWindowSecurityFrag(this, window)
        addSlide(IntroWelcomeFragment())
        addSlide(IntroPermissionFragment())
        addSlide(IntroSelectDirFragment())
        addSlide(IntroDoneFragment())

        askForPermissions(permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), slideNumber = 2, required = true)

        isSystemBackButtonLocked = true
        isSkipButtonEnabled = false
        findViewById<View>(R.id.next).visibility = View.INVISIBLE

    }

    override fun moveToNextSlide() = goToNextSlide()
    override fun onPageSelected(position: Int) {
        super.onPageSelected(position)
        setSwipeLock(true)
        if (position == 2) {
            findViewById<View>(R.id.next)?.visibility = View.INVISIBLE
        }
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        preferences.isFirstStart = false
        finish()
    }
}