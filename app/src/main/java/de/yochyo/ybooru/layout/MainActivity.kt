package de.yochyo.ybooru.layout

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.Api
import de.yochyo.ybooru.api.Tag
import de.yochyo.ybooru.database
import de.yochyo.ybooru.layout.res.Menus
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.setColor
import de.yochyo.ybooru.utils.toTagString
import de.yochyo.ybooru.utils.underline
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val selectedTags = ArrayList<String>()
    private lateinit var menu: Menu

    private lateinit var recycleView: RecyclerView
    private lateinit var adapter: SearchTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

        val navLayout = nav_search.findViewById<LinearLayout>(R.id.nav_search_layout)
        initAddTagButton(navLayout.findViewById(R.id.add_search))
        initSearchButton(navLayout.findViewById(R.id.start_search))
        recycleView = navLayout.findViewById(R.id.recycler_view_search)
        recycleView.layoutManager = LinearLayoutManager(this)
        adapter = SearchTagAdapter().apply { recycleView.adapter = this }
    }

    private fun initAddTagButton(b: Button) {
        b.setOnClickListener {
            var dialogIsDismissed = false
            val builder = AlertDialog.Builder(this)
            val layout = LayoutInflater.from(this).inflate(R.layout.search_item_dialog_view, null) as LinearLayout
            val editText = layout.findViewById<AutoCompleteTextView>(R.id.add_tag_edittext)
            val arrayAdapter = object : ArrayAdapter<Tag>(this@MainActivity, android.R.layout.simple_dropdown_item_1line) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val tag = getItem(position)
                    val textView = super.getView(position, convertView, parent) as TextView
                    if (tag != null) {
                        if (Build.VERSION.SDK_INT > 22) textView.setTextColor(getColor(tag.color))
                        else textView.setTextColor(resources.getColor(tag.color))
                    }
                    return textView
                }
            }

            editText.setAdapter(arrayAdapter)
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    editText.setAdapter(arrayAdapter) //Because of bug, that suggestions aren´t correctly updated
                    val name = s.toString()
                    GlobalScope.launch {
                        val tags = Api.searchTags(this@MainActivity, name)
                        launch(Dispatchers.Main) {
                            if (!dialogIsDismissed && editText.text.toString() == name) {
                                arrayAdapter.apply { clear(); addAll(tags); notifyDataSetChanged() }
                                if (tags.size == 1 && tags.first().name == name) editText.dismissDropDown()
                                else editText.showDropDown()
                            }
                        }
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    editText.dismissDropDown()
                    editText.setAdapter(null)//Because of bug, that suggestions aren´t correctly updated
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            })
            builder.setMessage("Add Tag").setView(layout)
            builder.setPositiveButton("OK") { _, _ ->
                if (database.getTag(editText.text.toString()) == null) {
                    GlobalScope.launch {
                        val tag = Api.getTag(this@MainActivity, editText.text.toString())
                        launch(Dispatchers.Main) {
                            val newTag: Tag
                            if (tag != null) newTag = database.addTag(tag.name, tag.type, tag.isFavorite)
                            else newTag = database.addTag(editText.text.toString(), Tag.UNKNOWN, false)
                            adapter.notifyItemInserted(database.getTags().indexOf(newTag))
                        }
                    }
                }
            }

            val dialog = builder.create()
            dialog.show()
            editText.requestFocus()
            dialog.setOnDismissListener { dialogIsDismissed = true }
            dialog.window?.setGravity(Gravity.TOP)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        }
    }
    private fun initSearchButton(b: Button) {
        b.setOnClickListener {
            drawer_layout.closeDrawer(GravityCompat.END)
            if (selectedTags.isEmpty()) PreviewActivity.startActivity(this, "*")
            else PreviewActivity.startActivity(this, selectedTags.toTagString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.main_menu, menu)
        setMenuR18Text()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_r18 -> {
                database.r18 = !database.r18
                setMenuR18Text()
                Manager.resetAll()
            }
            R.id.search -> drawer_layout.openDrawer(GravityCompat.END)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_subs -> startActivity(Intent(this, SubscriptionActivity::class.java))
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.community -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
            R.id.nav_help -> Toast.makeText(this, "Ask me some questions", Toast.LENGTH_SHORT).show()
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return super.onOptionsItemSelected(item)
    }

    private fun setMenuR18Text() {
        if (database.r18) menu.findItem(R.id.action_r18).title = getString(R.string.enter_r18)
        else menu.findItem(R.id.action_r18).title = getString(R.string.leave_r18)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) drawer_layout.closeDrawer(GravityCompat.START)
        else if (drawer_layout.isDrawerOpen(GravityCompat.END)) drawer_layout.closeDrawer(GravityCompat.END)
        else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        println("tag favorite ${database.sortTagsByFavorite}")
        println("tag alphabet ${database.sortTagsByAlphabet}")
        database.getTags().sort()
        adapter.notifyDataSetChanged()
    }

    private inner class SearchTagAdapter : RecyclerView.Adapter<SearchTagViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchTagViewHolder = SearchTagViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.search_item_layout, parent, false) as Toolbar)).apply {
            val check = toolbar.findViewById<CheckBox>(R.id.search_checkbox)
            toolbar.inflateMenu(R.menu.activity_main_search_menu)
            toolbar.setOnClickListener {
                if (check.isChecked) selectedTags.remove(it.findViewById<TextView>(R.id.search_textview).text)
                else selectedTags.add(it.findViewById<TextView>(R.id.search_textview).text.toString())
                check.isChecked = !check.isChecked
            }
            check.setOnClickListener {
                if (!(it as CheckBox).isChecked) selectedTags.remove(toolbar.findViewById<TextView>(R.id.search_textview).text)
                else selectedTags.add(toolbar.findViewById<TextView>(R.id.search_textview).text.toString())
            }
            toolbar.setOnMenuItemClickListener {
                val tag = database.getTags()[adapterPosition]
                when (it.itemId) {
                    R.id.main_search_favorite_tag -> {
                        database.changeTag(tag.apply { isFavorite = !isFavorite })
                        adapter.notifyItemChanged(adapterPosition)
                    }
                    R.id.main_search_subscribe_tag -> {
                        if (database.getSubscription(tag.name) == null) {
                            database.addSubscription(tag.name, 0)
                            Toast.makeText(this@MainActivity, "Subscribed ${tag.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            database.removeSubscription(tag.name)
                            Toast.makeText(this@MainActivity, "Unsubscribed ${tag.name}", Toast.LENGTH_SHORT).show()
                        }
                        adapter.notifyItemChanged(adapterPosition)
                    }
                    R.id.main_search_delete_tag -> {
                        database.removeTag(tag.name)
                        selectedTags.remove(tag.name)
                        database.removeSubscription(tag.name)
                        adapter.notifyItemRemoved(adapterPosition)
                    }
                }
                true
            }
        }

        override fun getItemCount(): Int = database.getTags().size
        override fun onBindViewHolder(holder: SearchTagViewHolder, position: Int) {
            val tag = database.getTags()[position]
            val textView = holder.toolbar.findViewById<TextView>(R.id.search_textview)
            textView.text = tag.name
            textView.setColor(tag)
            textView.underline(tag.isFavorite)

            Menus.initMainSearchTagMenu(this@MainActivity, holder.toolbar.menu, tag)
        }
    }

    private inner class SearchTagViewHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}
