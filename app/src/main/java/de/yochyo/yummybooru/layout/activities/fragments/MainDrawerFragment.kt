package de.yochyo.yummybooru.layout.activities.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.eventcollection.events.OnRemoveElementsEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.layout.activities.SettingsActivity
import de.yochyo.yummybooru.layout.activities.fragments.interfaces.IFragmentWithContainer
import de.yochyo.yummybooru.layout.activities.fragments.interfaces.OnBackPress
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.activities.subscriptionactivity.SubscriptionActivity
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddSpecialTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.utils.general.*
import kotlinx.android.synthetic.main.main_drawer_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainDrawerFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener, IFragmentWithContainer, OnBackPress {
    val selectedTags = ArrayList<String>()
    private val selectedTagRemovedListener = Listener.create<OnRemoveElementsEvent<Tag>> { selectedTags.removeAll(it.elements.map { it.name }) }
    private val selectedServerChangedEvent = Listener.create<SelectServerEvent> {
        if (it.oldServer != it.newServer)
            selectedTags.clear()
    }

    private lateinit var layout: DrawerLayout
    private var withContainer: ((container: ViewGroup) -> Unit)? = null

    private lateinit var filteringTagList: FilteringEventCollection<Tag, Int>
    suspend fun filter(name: String) {
        val result = filteringTagList.filter(name)
        withContext(Dispatchers.Main) {
            tagLayoutManager.scrollToPosition(0)
            tagAdapter.update(result)
        }
    }

    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var tagAdapter: TagAdapter
    private lateinit var tagLayoutManager: LinearLayoutManager

    companion object {
        private const val SELECTED = "SELECTED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx.db.tags.registerOnRemoveElementsListener(selectedTagRemovedListener)
        SelectServerEvent.registerListener(selectedServerChangedEvent)
        val array = savedInstanceState?.getStringArray(SELECTED)
        if (array != null)
            selectedTags += array
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        layout = (inflater.inflate(R.layout.main_drawer_fragment, container, false) as DrawerLayout).apply {
            if (withContainer != null) withContainer!!(this.findViewById(R.id.main_drawer_container))
        }

        filteringTagList = FilteringEventCollection({ ctx.db.tags }, { it.name })
        configureToolbar()
        configureTagDrawer()
        ctx.db.tags.registerOnUpdateListener { GlobalScope.launch(Dispatchers.Main) { tagAdapter.update(filteringTagList) } }
        return layout
    }

    override fun onDestroy() {
        ctx.db.tags.removeOnRemoveElementsListener(selectedTagRemovedListener)
        SelectServerEvent.removeListener(selectedServerChangedEvent)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(SELECTED, selectedTags.toTypedArray())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_server -> AddServerDialog {
                GlobalScope.launch {
                    ctx.db.servers += it
                    withContext(Dispatchers.Main) { Snackbar.make(drawer_layout, "Add server [${it.name}]", Snackbar.LENGTH_SHORT).show() }
                }
            }.build(ctx)
            R.id.search -> drawer_layout.openDrawer(GravityCompat.END)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun configureTagDrawer() {
        val navLayout = layout.findViewById<LinearLayout>(R.id.nav_search_layout)
        configureDrawerToolbar(navLayout.findViewById(R.id.search_toolbar))
        tagRecyclerView = navLayout.findViewById(R.id.recycler_view_search)
        tagRecyclerView.layoutManager = LinearLayoutManager(ctx).apply { tagLayoutManager = this }
        layout.findViewById<SearchView>(R.id.tag_filter).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) GlobalScope.launch { filter(newText) }
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })

        tagAdapter = TagAdapter(filteringTagList).apply { tagRecyclerView.adapter = this }
    }

    private fun configureToolbar() {
        val activity = requireActivity()
        val toolbar = layout.findViewById<Toolbar>(R.id.main_drawer_toolbar)
        if (activity is AppCompatActivity) {
            setHasOptionsMenu(true)
            activity.setSupportActionBar(toolbar)
        }
        val toggle = ActionBarDrawerToggle(activity, layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        layout.addDrawerListener(toggle)
        toggle.syncState()
        layout.findViewById<NavigationView>(R.id.nav_view).setNavigationItemSelectedListener(this)
    }

    private fun configureDrawerToolbar(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.main_search_nav_menu)
        toolbar.setNavigationIcon(R.drawable.clear)
        toolbar.navigationContentDescription = "Unselect all tags"
        toolbar.setNavigationOnClickListener {
            selectedTags.clear()
            Toast.makeText(ctx, "Unselected all tags", Toast.LENGTH_SHORT).show()
            tagAdapter.notifyDataSetChanged()
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search -> {
                    drawer_layout.closeDrawer(GravityCompat.END)
                    PreviewActivity.startActivity(ctx, if (selectedTags.isEmpty()) "*" else selectedTags.toTagString())
                }
                R.id.add_tag -> {
                    AddTagDialog {
                        GlobalScope.launch {
                            val t = ctx.currentServer.getTag(ctx, it.text.toString())
                            if (t != null) {
                                ctx.db.tags += t
                                withContext(Dispatchers.Main) {
                                    tagLayoutManager.scrollToPositionWithOffset(filteringTagList.indexOfFirst { it.name == t.name }, 0)
                                }
                            }
                        }
                    }.build(ctx)
                }
                R.id.add_special_tag -> AddSpecialTagDialog().build(ctx)
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_subs -> startActivity(Intent(ctx, SubscriptionActivity::class.java))
            R.id.nav_settings -> startActivity(Intent(ctx, SettingsActivity::class.java))
            R.id.community -> startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://discord.gg/tbGCHpF") })
            R.id.nav_help -> Toast.makeText(ctx, getString(R.string.join_discord), Toast.LENGTH_SHORT).show()
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return super.onOptionsItemSelected(item)
    }

    override fun withContainer(task: (container: ViewGroup) -> Unit) {
        if (!this::layout.isInitialized) withContainer = task
        else task(layout.findViewById(R.id.main_drawer_container))
    }

    override fun onBackPress(): Boolean {
        val drawerLayout = layout.findViewById<DrawerLayout>(R.id.drawer_layout)
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) -> drawerLayout.closeDrawer(GravityCompat.START)
            drawerLayout.isDrawerOpen(GravityCompat.END) -> drawerLayout.closeDrawer(GravityCompat.END)
            else -> return false
        }
        return true
    }

    inner class TagAdapter(t: Collection<Tag>) : RecyclerView.Adapter<TagViewHolder>() {
        private var tags: Collection<Tag> = t

        fun update(t: Collection<Tag>){
            tags = t
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder = TagViewHolder((LayoutInflater.from(context).inflate(R.layout.search_item_layout, parent, false) as android.widget.Toolbar)).apply {
            toolbar.inflateMenu(R.menu.activity_main_search_menu)
            val check = toolbar.findViewById<CheckBox>(R.id.search_checkbox)

            fun onClick() {
                if (check.isChecked) selectedTags.add(toolbar.findViewById<TextView>(R.id.search_textview).text.toString())
                else selectedTags.remove(toolbar.findViewById<TextView>(R.id.search_textview).text)
            }
            toolbar.setOnClickListener {
                check.isChecked = !check.isChecked
                onClick()
            }
            check.setOnClickListener {
                onClick()
            }

            toolbar.setOnMenuItemClickListener {
                val tag = tags.elementAt(adapterPosition)
                when (it.itemId) {
                    R.id.main_search_favorite_tag -> tag.isFavorite = !tag.isFavorite
                    R.id.main_search_subscribe_tag -> {
                        GlobalScope.launch {
                            if(tag.sub == null) tag.addSub(ctx)
                            else tag.sub = null
                            withContext(Dispatchers.Main) { notifyItemChanged(adapterPosition) }
                        }
                    }
                    R.id.main_search_delete_tag -> {
                        ConfirmDialog {
                            ctx.db.tags -= tag
                        }.withTitle("Delete").withMessage("Delete tag ${tag.name}").build(ctx)
                    }
                }
                true
            }
        }

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
            val tag = tags.elementAt(position)
            holder.toolbar.findViewById<CheckBox>(R.id.search_checkbox).isChecked = selectedTags.contains(tag.name)
            val textView = holder.toolbar.findViewById<TextView>(R.id.search_textview)
            textView.text = tag.name;textView.setColor(tag.color);textView.underline(tag.isFavorite)
            Menus.initMainSearchTagMenu(ctx, holder.toolbar.menu, tag)
        }

        override fun getItemCount(): Int = tags.size
    }

    class TagViewHolder(val toolbar: android.widget.Toolbar) : RecyclerView.ViewHolder(toolbar)
}