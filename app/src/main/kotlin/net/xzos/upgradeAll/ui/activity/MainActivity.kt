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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.navigation.NavigationView
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.ui.viewmodels.fragment.AppListFragment
import net.xzos.upgradeAll.ui.viewmodels.fragment.HubListFragment
import net.xzos.upgradeAll.utils.FileUtil
import java.io.File

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var menu: Menu

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
                        Log.e(LogObjectTag, TAG, "onActivityResult: 图片裁剪错误: $cropError")
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val headerView = navView.getHeaderView(0) as LinearLayout
        headerView.setOnClickListener {
            Toast.makeText(this, "长按侧滑栏图片可以删除图片", Toast.LENGTH_SHORT).show()
            setNavImage()
        }
        headerView.setOnLongClickListener {
            delNavImage()
            true
        }
        val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)
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
        setFrameLayout()
        deleteFile(NAV_IMAGE_FILE_NAME)
    }

    private fun setFrameLayout(id: Int = R.id.app_list) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val fragment = when (id) {
            R.id.app_list -> {
                if (::menu.isInitialized) {
                    menu.clear()
                    menuInflater.inflate(R.menu.menu_actionbar_app_list, menu)
                }
                AppListFragment()
            }
            R.id.hub_list -> {
                if (::menu.isInitialized) {
                    menu.clear()
                    menuInflater.inflate(R.menu.menu_actionbar_hub_list, menu)
                }
                HubListFragment()
            }
            else -> null
        }
        if (fragment != null) {
            fragmentTransaction.replace(R.id.contentFrameLayout, fragment)
            fragmentTransaction.commit()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_actionbar_app_list, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val intent: Intent

        return when (id) {
            R.id.item_add -> {
                intent = Intent(this@MainActivity, AppSettingActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.app_help -> {
                @Suppress("NAME_SHADOWING")
                var intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://xzos.net/upgradeall-developer-documentation/")
                intent = Intent.createChooser(intent, "请选择浏览器以查看帮助文档")
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        var intent: Intent

        when (id) {
            R.id.app_list -> {
                setFrameLayout(id)
            }
            R.id.hub_list -> {
                setFrameLayout(id)
            }
            R.id.item_add -> {
                intent = Intent(this, AppSettingActivity::class.java)
                startActivity(intent)
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
            super.onBackPressed()
        }
    }

    companion object {

        private val Log = ServerContainer.Log
        private const val TAG = "MainActivity"
        private val LogObjectTag = arrayOf("Core", TAG)

        private const val NAV_IMAGE_FILE_NAME = "nav_image.png"
        private val NAV_IMAGE_FILE = File(File(MyApplication.context.filesDir, "images"), NAV_IMAGE_FILE_NAME)

        private const val PERMISSIONS_REQUEST_WRITE_CONTACTS = 1
        private const val READ_PIC_REQUEST_CODE = 2
    }
}
