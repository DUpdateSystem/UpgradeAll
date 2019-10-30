package net.xzos.upgradeAll.ui.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.widget.ImageView
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
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.ui.viewmodels.fragment.AppInfoFragment
import net.xzos.upgradeAll.utils.FileUtil
import java.io.File

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var menu: Menu
    private lateinit var navController: NavController
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

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
        navigationItemId.observe(this, Observer { pair ->
            setFrameLayout(pair)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                READ_PIC_REQUEST_CODE -> {
                    val uri = resultData?.data
                    if (uri != null) {
                        val parent = NAV_IMAGE_FILE.parentFile
                        if (parent != null && !parent.exists())
                            parent.mkdirs()
                        val destinationUri = Uri.fromFile(NAV_IMAGE_FILE)
                        UCrop.of(uri, destinationUri)
                                .withAspectRatio(16f, 9f)
                                .start(this, UCrop.REQUEST_CROP)
                    }
                }
                UCrop.REQUEST_CROP -> renewNavImage()
                UCrop.RESULT_ERROR -> {
                    val cropError = UCrop.getError(resultData!!)
                    if (cropError != null)
                        Log.e(logObjectTag, TAG, "onActivityResult: 图片裁剪错误: $cropError")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_CONTACTS) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "设置背景图片需要读写本地文件", Toast.LENGTH_LONG).show()
            } else
                setNavImage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        return true
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
        navigationItemId.value = Pair(item.itemId, null)
        when (item.itemId) {
            R.id.app_list -> {
                navigationItemId.value = Pair(R.id.appListFragment, null)
            }
            R.id.cloud_hub_list -> {
                navigationItemId.value = Pair(R.id.hubCloudFragment, null)
            }
            R.id.local_hub_debug -> {
                if (::menu.isInitialized) {
                    menu.clear()
                    menuInflater.inflate(R.menu.menu_actionbar_debug, menu)
                }
                startActivity(Intent(this, HubDebugActivity::class.java))
            }
            R.id.app_help -> {
                intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://xzos.net/upgradeall-readme/")
                intent = Intent.createChooser(intent, "请选择浏览器")
                startActivity(intent)
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
            if (findNavController(R.id.nav_host_fragment).currentDestination?.id != R.id.appListFragment) {
                navigationItemId.value = Pair(R.id.appListFragment, null)
            } else
                super.onBackPressed()
        }
    }

    private fun setFrameLayout(pair: Pair<Int, Long?>) {
        actionBarDrawerToggle.isDrawerIndicatorEnabled = true  // 默认允许侧滑
        with(navController) {
            val currentDestination = this.currentDestination?.id
            if (currentDestination != null) {
                when (pair.first) {
                    R.id.appListFragment -> {
                        when (currentDestination) {
                            R.id.hubCloudFragment -> {
                                this.navigate(R.id.action_hubCloudFragment_to_appListFragment)
                                setToolbarByNavigation(R.id.appListFragment)
                            }
                            R.id.appInfoFragment -> this.navigate(R.id.action_appInfoFragment_to_appListFragment)
                        }

                    }
                    R.id.hubCloudFragment -> {
                        if (currentDestination == R.id.appListFragment) {
                            this.navigate(R.id.action_appListFragment_to_hubCloudFragment)
                            setToolbarByNavigation(R.id.hubCloudFragment)
                        }
                    }
                    R.id.appInfoFragment -> {
                        actionBarDrawerToggle.isDrawerIndicatorEnabled = false  // 禁止开启侧滑栏，启用返回按钮响应事件
                        pair.second?.let {
                            if (currentDestination == R.id.appListFragment)
                                this.navigate(R.id.action_appListFragment_to_appInfoFragment,
                                        Bundle().apply {
                                            putLong(AppInfoFragment.APP_DATABASE_ID, it)
                                        }
                                )
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
            setNavImage()
        }
        headerView.setOnLongClickListener {
            delNavImage()
            true
        }
        val navHeaderImageView = headerView.findViewById<ImageView>(R.id.navHeaderImageView)
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

    private fun setNavImage() {
        if (FileUtil.requestPermission(this, PERMISSIONS_REQUEST_WRITE_CONTACTS)) {
            FileUtil.getPicFormGallery(this, READ_PIC_REQUEST_CODE)
        }
    }

    private fun renewNavImage() {
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

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "MainActivity"
        private val logObjectTag = arrayOf("Core", TAG)

        private const val NAV_IMAGE_FILE_NAME = "nav_image.png"
        private val NAV_IMAGE_FILE = File(File(MyApplication.context.filesDir, "images"), NAV_IMAGE_FILE_NAME)

        private const val PERMISSIONS_REQUEST_WRITE_CONTACTS = 1
        private const val READ_PIC_REQUEST_CODE = 2

        // Pair(Fragment ID, Bundle)
        internal val navigationItemId: MutableLiveData<Pair<Int, Long?>> = MutableLiveData(Pair(R.id.appListFragment, null))
    }
}