package de.yochyo.yummybooru.layout.activities.introactivities.savefolderactivity

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro2
import com.github.appintro.R
import de.yochyo.yummybooru.layout.activities.introactivities.IIntroActivity
import de.yochyo.yummybooru.layout.activities.introactivities.introactivity.IntroDoneFragment
import de.yochyo.yummybooru.layout.activities.introactivities.introactivity.IntroSelectDirFragment

class SaveFolderChangerActivity : AppIntro2(), IIntroActivity {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addSlide(
            IntroSelectDirFragment(
                "Directory not found", "The save directory was deleted or moved, please select a new one. If you just booted android, close this app and " +
                        "open it again in a minute."
            )
        )
        addSlide(IntroDoneFragment())

        isSystemBackButtonLocked = true
        isSkipButtonEnabled = false
        findViewById<View>(R.id.next).visibility = View.INVISIBLE
    }

    override fun onPageSelected(position: Int) {
        super.onPageSelected(position)
        setSwipeLock(true)
        if (position == 0) {
            findViewById<View>(R.id.next)?.visibility = View.INVISIBLE
        }
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finish()
    }

    override fun moveToNextSlide() = goToNextSlide()
}