package de.yochyo.yummybooru.layout.activities.welcomeactivity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.activities.mainactivity.MainActivity
import kotlinx.android.synthetic.main.intro_activity_select_save_folder_layout.*
import kotlinx.coroutines.*

class WaitingForSystemActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.intro_activity_select_save_folder_layout)
        findViewById<TextView>(R.id.title).text = getString(R.string.app_name)
        description.text = "Android is still booting"

        GlobalScope.launch {
            var savePath = db.saveFolder
            while (!savePath.exists()) {
                savePath = db.saveFolder
                delay(2000)
            }
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@WaitingForSystemActivity, MainActivity::class.java))
                finish()
            }
        }
    }

}