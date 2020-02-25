package net.xzos.upgradeall.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.dupdatesystem.data.json.nongson.ObjectTag
import net.xzos.dupdatesystem.server_manager.runtime.manager.module.app.App
import net.xzos.upgradeall.R
import net.xzos.upgradeall.server.update.UpdateManager
import net.xzos.upgradeall.utils.FileUtil.NAV_IMAGE_FILE
import net.xzos.upgradeall.utils.MiscellaneousUtils
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var navController: NavController

    init {

        UpdateManager.renewAll()

        navigationItemId = MutableLiveData<Int>(R.id.appListFragment).apply {
            this.observe(this@MainActivity, Observer { itemId ->
                setFrameLayout(itemId)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        navController = findNavController(R.id.nav_host_fragment)
        setToolbarByNavigation(null)
        toolbar.title = with(applicationInfo) {
            getString(this.labelRes)
        }
        setNavController()
        setNavHeaderView()
        navView.setNavigationItemSelectedListener(this)
        showToast()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.dev_help -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://xzos.net/upgradeall-developer-documentation/")
                }
                startActivity(
                        Intent.createChooser(intent, "请选择浏览器以查看帮助文档")
                )
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        navigationItemId.value = item.itemId
        when (item.itemId) {
            R.id.app_list -> {
                navigationItemId.value = R.id.appListFragment
            }
            R.id.cloud_hub_list -> {
                navigationItemId.value = R.id.hubCloudFragment
            }
            R.id.local_hub_debug -> {
                startActivity(Intent(this, HubDebugActivity::class.java))
            }
            R.id.app_help -> {
                MiscellaneousUtils.accessByBrowser(
                        "https://xzos.net/upgradeall-readme/",
                        this
                )
            }
            R.id.app_log -> {
                intent = Intent(this, LogActivity::class.java)
                startActivity(intent)
            }
            R.id.app_setting -> {
                intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // 判断是否是云端仓库页，如果是，则跳转软件列表页
            val currentDestination = navController.currentDestination?.id
            if (currentDestination != null && currentDestination == R.id.hubCloudFragment) {
                navigationItemId.value = R.id.appListFragment
            } else
                super.onBackPressed()
        }
    }

    private fun setFrameLayout(fragmentId: Int) {
        with(navController) {
            val currentDestination = this.currentDestination?.id
            if (currentDestination != null && currentDestination != fragmentId) {
                when (fragmentId) {
                    R.id.appListFragment -> {
                        if (currentDestination == R.id.hubCloudFragment) {
                            this.navigate(R.id.action_hubCloudFragment_to_appListFragment)
                            setToolbarByNavigation(R.id.appListFragment)
                        } else {
                            navController.popBackStack(R.id.appListFragment, false)
                        }
                    }
                    R.id.hubCloudFragment -> {
                        if (currentDestination != R.id.appListFragment)
                            navController.popBackStack(R.id.appListFragment, false)
                        this.navigate(R.id.action_appListFragment_to_hubCloudFragment)
                        setToolbarByNavigation(R.id.hubCloudFragment)
                    }
                    R.id.appInfoFragment -> {
                        bundleApp.also {
                            bundleApp = it
                        }?.let {
                            if (currentDestination == R.id.appListFragment) {
                                this.navigate(R.id.action_appListFragment_to_appInfoFragment)
                            }
                        }
                    }
                    R.id.appSettingFragment -> {
                        when (currentDestination) {
                            R.id.appListFragment -> {
                                this.navigate(R.id.action_appListFragment_to_appSettingFragment)
                            }
                            R.id.appInfoFragment -> {
                                bundleApp.also {
                                    bundleApp = it
                                }?.let {
                                    this.navigate(R.id.action_appInfoFragment_to_appSettingFragment)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setToolbarByNavigation(startDestination: Int?) {
        with(navController) {
            this.graph = graph.apply {
                if (startDestination != null && startDestination in listOf(R.id.appListFragment, R.id.hubCloudFragment))
                    this.startDestination = startDestination
            }
            NavigationUI.setupActionBarWithNavController(this@MainActivity, this,
                    AppBarConfiguration.Builder(navController.graph).setDrawerLayout(drawerLayout).build()
            )
        }
    }

    private fun setNavHeaderView() {
        val headerView = navView.getHeaderView(0) as LinearLayout
        headerView.setOnClickListener {
            Toast.makeText(this, "长按侧滑栏图片可以删除图片", Toast.LENGTH_SHORT).show()
            GlobalScope.launch {
                if (UCropActivity.newInstance(19f, 6f, NAV_IMAGE_FILE, this@MainActivity))
                    withContext(Dispatchers.Main) {
                        renewNavImage()
                    }
            }
        }
        headerView.setOnLongClickListener {
            delNavImage()
            true
        }
        val navHeaderImageView = headerView.navHeaderImageView
        navHeaderImageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                navHeaderImageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val layoutParams = navHeaderImageView.layoutParams as LinearLayout.LayoutParams
                layoutParams.height = navHeaderImageView.width / 16 * 9
                navHeaderImageView.layoutParams = layoutParams
                renewNavImage()
            }
        })
    }

    private fun setNavController() {
        actionBarDrawerToggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        actionBarDrawerToggle.setToolbarNavigationClickListener {
            onBackPressed()
        }
    }

    private fun renewNavImage() {
        if (NAV_IMAGE_FILE.exists())
            Glide.with(this)
                    .load(NAV_IMAGE_FILE)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(navHeaderImageView)
    }

    private fun delNavImage() {
        NAV_IMAGE_FILE.delete()
        navHeaderImageView.setImageDrawable(null)
    }

    private fun showToast() {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DATE)
        if (month == 10 && day == 31)
            Toast.makeText(this, "\uD83E\uDD70\uD83D\uDE0B\uD83D\uDE1D\uD83D\uDE09", Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "MainActivity"
        private val logObjectTag = ObjectTag("UI", TAG)

        // Fragment 跳转
        internal lateinit var navigationItemId: MutableLiveData<Int>
        internal var bundleApp: App? = null
            get() {
                val app = field
                field = null
                return app
            }

        internal lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    }
}
