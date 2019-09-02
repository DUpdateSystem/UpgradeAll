package net.xzos.upgradeAll.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import io.github.kobakei.materialfabspeeddial.FabSpeedDial
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.log.LogDataProxy
import net.xzos.upgradeAll.ui.viewmodels.log.SectionsPagerAdapter
import net.xzos.upgradeAll.utils.FileUtil
import java.io.File
import java.io.IOException

class LogActivity : AppCompatActivity() {

    private var logSort = "Core"

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
                Toast.makeText(this@LogActivity, "导出日志文件需要读写本地文件", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setFab() {
        val fab = findViewById<FabSpeedDial>(R.id.sortFab)
        val liveDataLogSortList = LogDataProxy(Log).liveDataLogSortList
        liveDataLogSortList.observe(this, Observer { logSortList ->
            val menu = FabSpeedDialMenu(this)
            for (logSort in logSortList) {
                if (logSort == "Core")
                    menu.add(resources.getString(R.string.main_program)).setIcon(R.drawable.ic_core)
                else
                    menu.add(logSort).setIcon(R.drawable.ic_cloud)
            }
            fab.setMenu(menu)
            fab.addOnMenuItemClickListener { _, _, integer ->
                logSort = logSortList[integer - 1]
                setViewPage(logSort)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
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
            R.id.log_clear -> {
                vItem = findViewById(R.id.log_clear)
                popupMenu = PopupMenu(this, vItem)
                menuInflater = popupMenu.menuInflater
                menuInflater.inflate(R.menu.menu_del_button, popupMenu.menu)
                popupMenu.show()
                //设置item的点击事件
                popupMenu.setOnMenuItemClickListener { popItem ->
                    val logDataProxy = LogDataProxy(Log)
                    when (popItem.itemId) {
                        // 清空当前分类的日志
                        R.id.log_del_sort -> {
                            logDataProxy.clearLogSort(logSort)
                            setViewPage(logSort)
                            setFab()
                        }
                        // 清空全部日志
                        R.id.log_del_all -> {
                            logDataProxy.clearLogAll()
                            setViewPage(logSort)
                            setFab()
                        }
                    }
                    true
                }
                return true
            }
            R.id.log_share -> {
                if (ContextCompat.checkSelfPermission(this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    FileUtil.requestPermission(this, PERMISSIONS_REQUEST_WRITE_CONTACTS)
                    return true
                }
                vItem = findViewById(R.id.log_share)
                popupMenu = PopupMenu(this, vItem)
                menuInflater = popupMenu.menuInflater
                menuInflater.inflate(R.menu.menu_share_button, popupMenu.menu)
                popupMenu.show()
                //设置item的点击事件
                val logFilePath = "/sdcard/Download/UpgradeAll/"
                val logFileName = "Log.txt"
                popupMenu.setOnMenuItemClickListener { popItem ->
                    var logString: String? = null
                    val logDataProxy = LogDataProxy(Log)
                    when (popItem.itemId) {
                        // 导出当前分类日志
                        R.id.log_share_sort -> logString = logDataProxy.getLogStringBySort(logSort)
                        // 导出全部日志
                        R.id.log_share_all -> logString = logDataProxy.logAllToString
                    }
                    if (logString != null) {
                        Log.d(LogObjectTag, TAG, "已获取日志")
                        val logFile = File(logFilePath + logFileName)
                        val dir = File(logFilePath)
                        if (!FileUtil.fileIsExistsByPath(logFilePath)) {
                            Log.d(LogObjectTag, TAG, "创建日志目录")
                            if (dir.mkdirs())
                                Log.d(LogObjectTag, TAG, "已创建日志目录")
                        }
                        if (!FileUtil.fileIsExistsByPath(logFilePath + logFileName)) {
                            try {
                                logFile.createNewFile()
                            } catch (e: IOException) {
                                Log.e(LogObjectTag, TAG, "创建文件异常: ERROR_MESSAGE: $e")
                            }

                        }
                        if (FileUtil.writeTextFromUri(Uri.fromFile(logFile), logString))
                            Toast.makeText(this, "已导出日志至: $logFilePath$logFileName", Toast.LENGTH_LONG).show()
                        else
                            Toast.makeText(this, "日志导出失败", Toast.LENGTH_LONG).show()
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

    private fun setViewPage(sort: String) {
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager, sort)
        val viewPager = findViewById<ViewPager>(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs = findViewById<TabLayout>(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
    }

    companion object {
        private const val TAG = "LogActivity"
        private val LogObjectTag = arrayOf("Core", TAG)
        private val Log = ServerContainer.Log

        private const val PERMISSIONS_REQUEST_WRITE_CONTACTS = 1
    }
}