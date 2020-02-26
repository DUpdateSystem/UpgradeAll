package net.xzos.upgradeall.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu
import kotlinx.android.synthetic.main.activity_log.*
import net.xzos.dupdatesystem.core.data.json.nongson.ObjectTag
import net.xzos.dupdatesystem.core.log.Log
import net.xzos.dupdatesystem.core.log.LogDataProxy
import net.xzos.upgradeall.R
import net.xzos.upgradeall.server.log.LogLiveData
import net.xzos.upgradeall.ui.viewmodels.pageradapter.LogTabSectionsPagerAdapter
import net.xzos.upgradeall.utils.FileUtil

class LogActivity : AppCompatActivity() {

    private var logSort = "Core"
    private var logFileString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        // 设置 ActionBar
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f // 关闭toolbar阴影
        }

        setFab()
        setViewPage(logSort)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_CONTACTS) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "导出日志文件需要读写本地文件", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null && logFileString != null) {
                if (FileUtil.writeTextFromUri(uri, logFileString!!)) {
                    Toast.makeText(this, "已导出日志至", Toast.LENGTH_LONG).show()
                } else
                    Toast.makeText(this, "日志导出失败", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_actionbar_log, menu)
        return true
    }

    @SuppressLint("SdCardPath")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        val vItem: View
        val popupMenu: PopupMenu
        val menuInflater: MenuInflater
        when (id) {
            R.id.log_clean -> {
                vItem = findViewById(R.id.log_clean)
                popupMenu = PopupMenu(this, vItem)
                menuInflater = popupMenu.menuInflater
                menuInflater.inflate(R.menu.menu_del_button, popupMenu.menu)
                popupMenu.show()
                //设置item的点击事件
                popupMenu.setOnMenuItemClickListener { popItem ->
                    when (popItem.itemId) {
                        // 清空当前分类的日志
                        R.id.log_del_sort -> {
                            LogDataProxy.clearLogBySort(logSort)
                        }
                        // 清空全部日志
                        R.id.log_del_all -> {
                            LogDataProxy.clearLogAll()
                        }
                    }
                    setViewPage(logSort)
                    setFab()
                    true
                }
                return true
            }
            R.id.log_share -> {
                vItem = findViewById(R.id.log_share)
                popupMenu = PopupMenu(this, vItem)
                menuInflater = popupMenu.menuInflater
                menuInflater.inflate(R.menu.menu_share_button, popupMenu.menu)
                popupMenu.show()
                //设置item的点击事件
                popupMenu.setOnMenuItemClickListener { popItem ->
                    var logString: String? = null
                    when (popItem.itemId) {
                        // 导出当前分类日志
                        R.id.log_share_sort -> logString = LogDataProxy.getLogStringBySort(logSort)
                        // 导出全部日志
                        R.id.log_share_all -> logString = LogDataProxy.logAllToString
                    }
                    if (logString != null) {
                        Log.d(objectTag, TAG, "已获取日志")
                        FileUtil.createFile(this, WRITE_LOG_REQUEST_CODE, "text/plain", "Log.txt")
                        logFileString = logString
                    }
                    true
                }
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setFab() {
        val liveDataLogSortList = LogLiveData.sortList
        liveDataLogSortList.observe(this, Observer { logSortList ->
            val menu = FabSpeedDialMenu(this)
            for (logSort in logSortList) {
                if (logSort == "Core")
                    menu.add(resources.getString(R.string.main_program)).setIcon(R.drawable.ic_core)
                else
                    menu.add(logSort).setIcon(R.drawable.ic_cloud)
            }
            sortFab.setMenu(menu)
            sortFab.addOnMenuItemClickListener { _, _, integer ->
                logSort = logSortList.toList()[integer - 1]
                setViewPage(logSort)
            }
        })
    }


    private fun setViewPage(sort: String) {
        val sectionsPagerAdapter = LogTabSectionsPagerAdapter(this, supportFragmentManager, sort)
        viewPager.adapter = sectionsPagerAdapter
        logTabs.setupWithViewPager(viewPager)
    }

    companion object {
        private const val TAG = "LogActivity"
        private val objectTag = ObjectTag("UI", TAG)

        private const val PERMISSIONS_REQUEST_WRITE_CONTACTS = 1
        private const val WRITE_LOG_REQUEST_CODE = 2
    }
}