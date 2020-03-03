package net.xzos.upgradeall.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu
import kotlinx.android.synthetic.main.activity_log.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.dupdatesystem.core.data.json.nongson.ObjectTag
import net.xzos.dupdatesystem.core.log.Log
import net.xzos.dupdatesystem.core.log.LogDataProxy
import net.xzos.upgradeall.R
import net.xzos.upgradeall.server.log.LogLiveData
import net.xzos.upgradeall.ui.viewmodels.pageradapter.LogTabSectionsPagerAdapter

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
                    val logString = when (popItem.itemId) {
                        // 导出当前分类日志
                        R.id.log_share_sort -> LogDataProxy.getLogStringBySort(logSort)
                        // 导出全部日志
                        R.id.log_share_all -> LogDataProxy.logAllToString
                        else -> null
                    }
                    if (logString != null) {
                        Log.d(objectTag, TAG, "已获取日志")
                        GlobalScope.launch {
                            SaveFileActivity.newInstance(
                                    "Log.txt", logString.toByteArray(),
                                    "text/plain", this@LogActivity
                            )
                        }
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
    }
}