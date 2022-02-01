package net.xzos.upgradeall.ui.log

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import com.google.android.material.tabs.TabLayoutMediator
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.LogDataProxy
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.databinding.ActivityLogBinding
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.ui.utils.file_pref.SaveFileActivity

class LogActivity : AppBarActivity() {

    private var logSort = "Core"
    private lateinit var binding: ActivityLogBinding

    override fun initBinding(): View {
        binding = ActivityLogBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {
        setFab()
        setViewPage(logSort)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_actionbar_log, menu)
        return true
    }

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
                            AlertDialog.Builder(this)
                                .setTitle(R.string.clean_sort_log)
                                .setMessage(R.string.clean_sort_log_alert_message)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    LogDataProxy.clearLogBySort(logSort)
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                        }
                        // 清空全部日志
                        R.id.log_del_all -> {
                            AlertDialog.Builder(this)
                                .setTitle(R.string.clean_all_log)
                                .setMessage(R.string.clean_all_log_alert_message)
                                .setPositiveButton(android.R.string.ok) { _, _ -> LogDataProxy.clearLogAll() }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
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
                                "Log.txt", "text/plain",
                                logString.toByteArray(), this@LogActivity
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
        liveDataLogSortList.observe(this, { logSortList ->
            val menu = FabSpeedDialMenu(this)
            for (logSort in logSortList) {
                if (logSort == "Core") {
                    menu.add(resources.getString(R.string.main_program)).setIcon(R.drawable.ic_core)
                } else {
                    menu.add(logSort).setIcon(R.drawable.ic_cloud)
                }
            }
            binding.sortFab.setMenu(menu)
            binding.sortFab.addOnMenuItemClickListener { _, _, integer ->
                logSort = logSortList.toList()[integer - 1]
                setViewPage(logSort)
            }
        })
    }

    private fun setViewPage(sort: String) {
        val sectionsPagerAdapter = LogTabSectionsPagerAdapter(this, this, sort)
        binding.viewPager.adapter = sectionsPagerAdapter
        val mediator = TabLayoutMediator(binding.logTabs, binding.viewPager) { tab, position ->
            tab.text = sectionsPagerAdapter.getPageTitle(position)
        }
        mediator.attach()
    }

    companion object {
        private const val TAG = "LogActivity"
        private val objectTag = ObjectTag("UI", TAG)
    }
}