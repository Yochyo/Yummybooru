package de.yochyo.yummybooru.layout.activities.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
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
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.activities.SettingsActivity
import de.yochyo.yummybooru.layout.activities.mainactivity.MainActivity
import de.yochyo.yummybooru.layout.activities.mainactivity.TagAdapter
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.activities.subscriptionactivity.SubscriptionActivity
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddSpecialTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.utils.general.FilteringEventCollection
import de.yochyo.yummybooru.utils.general.ctx
import de.yochyo.yummybooru.utils.general.currentServer
import de.yochyo.yummybooru.utils.general.toTagString
import kotlinx.android.synthetic.main.main_drawer_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainDrawerFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener, IFragment {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val l = inflater.inflate(R.layout.main_drawer_fragment, container, false) as DrawerLayout
        if (withContainer != null) withContainer!!(l.findViewById(R.id.main_drawer_container))
        layout = l
        configureToolbar()
        val navLayout = layout.findViewById<LinearLayout>(R.id.nav_search_layout)
        configureDrawerToolbar(navLayout.findViewById(R.id.search_toolbar))
        tagRecyclerView = navLayout.findViewById(R.id.recycler_view_search)
        tagRecyclerView.layoutManager = LinearLayoutManager(ctx).apply { tagLayoutManager = this }
        l.findViewById<SearchView>(R.id.tag_filter).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) GlobalScope.launch { filter(newText) }
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })
        filteringTagList = FilteringEventCollection({ ctx.db.tags }, { it.name })
        tagAdapter = TagAdapter(ctx, filteringTagList).apply { tagRecyclerView.adapter = this }
        ctx.db.tags.registerOnUpdateListener { GlobalScope.launch(Dispatchers.Main) { tagAdapter.update(filteringTagList) } }
        return layout
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
            MainActivity.selectedTags.clear()
            Toast.makeText(ctx, "Unselected all tags", Toast.LENGTH_SHORT).show()
            tagAdapter.notifyDataSetChanged()
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search -> {
                    drawer_layout.closeDrawer(GravityCompat.END)
                    PreviewActivity.startActivity(ctx, if (MainActivity.selectedTags.isEmpty()) "*" else MainActivity.selectedTags.toTagString())
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
}