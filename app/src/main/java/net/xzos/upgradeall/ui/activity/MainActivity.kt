package net.xzos.upgradeall.ui.activity

import android.content.Intent
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
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.server.update.UpdateService
import net.xzos.upgradeall.ui.activity.file_pref.UCropActivity
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.ToastUtil
import net.xzos.upgradeall.utils.file.FileUtil.NAV_IMAGE_FILE
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val navController by lazy { findNavController(R.id.nav_host_fragment) }

    init {
        navigationItemId = MutableLiveData(R.id.appListFragment).apply {
            this.observe(this@MainActivity, Observer { itemId ->
                setFrameLayout(itemId)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        UpdateService.startService(this)
        setToolbarByNavigation(null)
        toolbar.title = with(applicationInfo) {
            getString(this.labelRes)
        }
        setNavController()
        setNavHeaderView()
        navView.setNavigationItemSelectedListener(this)
        showToast()
        PreferencesMap.initByActivity(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.dev_help -> {
                MiscellaneousUtils.accessByBrowser(
                        "https://xzos.net/upgradeall-developer-documentation/",
                        this
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

    /**
     * 侧滑栏选择
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navigationItemId = item.itemId
        setNavigationItemId(navigationItemId)
        when (navigationItemId) {
            R.id.app_list -> {
                setNavigationItemId(R.id.appListFragment)
            }
            R.id.cloud_hub_list -> {
                setNavigationItemId(R.id.hubCloudFragment)
            }
            R.id.app_help -> {
                MiscellaneousUtils.accessByBrowser(
                        getString(R.string.readme_url),
                        this
                )
            }
            R.id.app_log -> {
                startActivity(Intent(this, LogActivity::class.java))
            }
            R.id.app_setting -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        when {
            //抽屉为开启状态
            drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            //Tab 为编辑模式
            AppTabSectionsPagerAdapter.editTabMode.value ?: false -> {
                AppTabSectionsPagerAdapter.editTabMode.value = false
            }
            // 判断是否是云端仓库页，如果是，则跳转软件列表页
            navController.currentDestination?.id == R.id.hubCloudFragment -> {
                setNavigationItemId(R.id.appListFragment)
            }
            //退出
            else -> super.onBackPressed()
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
                        when (currentDestination) {
                            R.id.appListFragment ->
                                this.navigate(R.id.action_appListFragment_to_appInfoFragment)
                            R.id.applicationsFragment ->
                                this.navigate(R.id.action_applicationsFragment_to_appInfoFragment)
                        }
                    }
                    R.id.appSettingFragment -> {
                        when (currentDestination) {
                            R.id.appListFragment ->
                                this.navigate(R.id.action_appListFragment_to_appSettingFragment)
                            R.id.appInfoFragment ->
                                this.navigate(R.id.action_appInfoFragment_to_appSettingFragment)
                        }
                    }
                    R.id.applicationsFragment -> {
                        when (currentDestination) {
                            R.id.appListFragment -> {
                                this.navigate(R.id.action_appListFragment_to_applicationFragment)
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
                    AppBarConfiguration.Builder(navController.graph).setOpenableLayout(drawerLayout).build()
            )
        }
    }

    private fun setNavHeaderView() {
        val headerView = navView.getHeaderView(0) as LinearLayout
        headerView.setOnClickListener {
            ToastUtil.makeText(R.string.long_click_to_delete_image)
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
            ToastUtil.makeText("\uD83E\uDD70\uD83D\uDE0B\uD83D\uDE1D\uD83D\uDE09", Toast.LENGTH_LONG)
    }

    companion object {

        // Fragment 跳转
        private lateinit var navigationItemId: MutableLiveData<Int>
        internal fun setNavigationItemId(navigationItemId: Int) {
            this.navigationItemId.value = navigationItemId
        }

        internal lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    }
}
