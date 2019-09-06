package net.xzos.upgradeAll.ui.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.navigation.NavigationView
import com.yalantis.ucrop.UCrop
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.json.cache.ItemCardViewExtraData
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.ui.viewmodels.adapters.AppItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeAll.utils.FileUtil
import org.litepal.LitePal
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var enableRenew = true

    private val itemCardViewList = ArrayList<ItemCardView>()

    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var navViewHeaderImageView: ImageView
    private lateinit var adapter: AppItemAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                READ_PIC_REQUEST_CODE -> {
                    val uri = resultData!!.data
                    if (uri != null) {
                        val parent = NAV_IMAGE_FILE.parentFile
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs()
                        }
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

    override fun onResume() {
        super.onResume()
        if (enableRenew) {
            refreshCardView()
            enableRenew = false
        }
        navView.setCheckedItem(R.id.app_list)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        recyclerView = findViewById(R.id.update_item_recycler_view)
        mDrawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        val headerView = navView.getHeaderView(0) as LinearLayout
        navViewHeaderImageView = headerView.findViewById(R.id.nav_header_imageView)
        headerView.setOnClickListener {
            Toast.makeText(this@MainActivity, "长按侧滑栏图片可以删除图片", Toast.LENGTH_SHORT).show()
            setNavImage()
        }
        headerView.setOnLongClickListener {
            delNavImage()
            true
        }
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        swipeRefresh.setOnRefreshListener { this.refreshCardView() }
        val toggle = ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        mDrawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)
        navViewHeaderImageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                navViewHeaderImageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val layoutParams = navViewHeaderImageView.layoutParams as LinearLayout.LayoutParams
                layoutParams.height = navViewHeaderImageView.width / 16 * 9
                navViewHeaderImageView.layoutParams = layoutParams
                renewNavImage()
            }
        })
        setRecyclerView()
        deleteFile(NAV_IMAGE_FILE_NAME)
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
                .into(navViewHeaderImageView)
    }

    private fun delNavImage() {
        NAV_IMAGE_FILE.delete()
        navViewHeaderImageView.setImageDrawable(null)
    }

    private fun refreshCardView() {
        swipeRefresh.isRefreshing = true
        refreshAppList()
        swipeRefresh.isRefreshing = false
    }

    private fun refreshAppList() {
        val repoDatabase = LitePal.findAll(RepoDatabase::class.java)
        itemCardViewList.clear()
        for (updateItem in repoDatabase) {
            val databaseId = updateItem.id
            val name = updateItem.name
            val api = updateItem.api
            val url = updateItem.url
            itemCardViewList.add(ItemCardView(name, url, api, ItemCardViewExtraData(databaseId = databaseId)))
        }
        val guidelinesTextView = findViewById<TextView>(R.id.guidelinesTextView)
        if (itemCardViewList.size != 0) {
            itemCardViewList.add(ItemCardView(null, null, null, ItemCardViewExtraData(isEmpty = true)))
            setRecyclerView()
            adapter.notifyDataSetChanged()
            guidelinesTextView.visibility = View.GONE
        } else {
            guidelinesTextView.visibility = View.VISIBLE
        }
    }

    private fun setRecyclerView() {
        val layoutManager = GridLayoutManager(this, 1)
        recyclerView.layoutManager = layoutManager
        adapter = AppItemAdapter(itemCardViewList)
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_actionbar_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val intent: Intent

        return if (id == R.id.item_add) {
            enableRenew = true
            intent = Intent(this@MainActivity, AppSettingActivity::class.java)
            startActivity(intent)
            true
        } else
            super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        var intent: Intent

        when (id) {
            R.id.item_add -> {
                enableRenew = true
                intent = Intent(this@MainActivity, AppSettingActivity::class.java)
                startActivity(intent)
            }
            R.id.app_help -> {
                intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://xzos.net/upgradeall-readme/")
                intent = Intent.createChooser(intent, "请选择浏览器")
                startActivity(intent)
            }
            R.id.hub_list -> {
                intent = Intent(this@MainActivity, HubListActivity::class.java)
                startActivity(intent)
            }
            R.id.app_log -> {
                intent = Intent(this@MainActivity, LogActivity::class.java)
                startActivity(intent)
            }
            R.id.app_setting -> {
                intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        mDrawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        mDrawerLayout = findViewById(R.id.drawer_layout)
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START)
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
