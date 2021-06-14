package net.xzos.upgradeall.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.absinthe.libraries.utils.extensions.addPaddingBottom
import com.absinthe.libraries.utils.extensions.addPaddingTop
import com.absinthe.libraries.utils.utils.UiUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.core.utils.oberver.ObserverFunNoArg
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.databinding.ActivityMainBinding
import net.xzos.upgradeall.server.update.UpdateService
import net.xzos.upgradeall.ui.applist.apps.AppsActivity
import net.xzos.upgradeall.ui.applist.magisk.MagiskModuleActivity
import net.xzos.upgradeall.ui.base.BaseActivity
import net.xzos.upgradeall.ui.discover.DiscoverActivity
import net.xzos.upgradeall.ui.filemanagement.FileManagementActivity
import net.xzos.upgradeall.ui.home.adapter.*
import net.xzos.upgradeall.ui.hubmanager.HubManagerActivity
import net.xzos.upgradeall.ui.log.LogActivity
import net.xzos.upgradeall.ui.preference.SettingsActivity
import net.xzos.upgradeall.utils.ToastUtil
import net.xzos.upgradeall.utils.UxUtils
import net.xzos.upgradeall.utils.runUiFun


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val observer: ObserverFunNoArg = { renewUpdateStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()

        checkUpdate()
        PreferencesMap.initByActivity(this)
    }

    private fun initView() {
        with(binding.layoutTitleBar.tabName) {
            val observer: ViewTreeObserver = this.viewTreeObserver
            observer.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    this@with.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    renewTitle()
                }
            })

        }
        with(binding.layoutTitleBar.tabName) {
            text = UxUtils.getAppTitle(this@MainActivity, this)
        }
        val homeAdapter = HomeModuleAdapter()
        binding.apply {
            rvModules.apply {
                adapter = homeAdapter
                setHasFixedSize(true)
                addPaddingBottom(UiUtils.getNavBarHeight(windowManager))
            }
            layoutTitleBar.root.addPaddingTop(UxUtils.getStatusBarHeight(resources))
        }
        val moduleList: MutableList<HomeModuleBean> =
            PreferencesMap.home_bottom_queue.mapNotNull { idToBean(it) }.toMutableList()
        if (!PreferencesMap.enable_simple_bottom_main) {
            moduleList.addAll(listOf(
                HomeModuleNonCardBean(R.drawable.ic_home_log, R.string.home_log) {
                    startActivity(Intent(this, LogActivity::class.java))
                },
                HomeModuleNonCardBean(R.drawable.ic_home_setting, R.string.home_settings) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                },
                HomeModuleNonCardBean(R.drawable.ic_home_about, R.string.home_about) {
                    ToastUtil.makeText(R.string.home_about)
                }
            ))
        } else {
            moduleList.add(HomeSimpleCardBean())
        }
        homeAdapter.setList(moduleList)
        binding.layoutUpdatingCard.apply {
            layoutCard.setOnClickListener {
                checkUpdate()
            }
        }
    }

    private fun renewTitle() {
        GlobalScope.launch {
            with(binding.layoutTitleBar.tabName) {
                val appTitle = UxUtils.getAppTitle(this@MainActivity, this)
                runUiFun { text = appTitle }
            }
        }
    }

    private fun idToBean(id: String): HomeModuleCardBean? {
        return when (id) {
            HOME_MODULE_DISCOVERY ->
                HomeModuleCardBean(R.drawable.ic_home_discovery, R.string.home_module_discovery) {
                    startActivity(Intent(this, DiscoverActivity::class.java))
                }
            HOME_MODULE_HUB_MANAGER ->
                HomeModuleCardBean(R.drawable.ic_home_hub, R.string.app_hub) {
                    startActivity(Intent(this, HubManagerActivity::class.java))
                }
            HOME_MODULE_FILE_MANAGER ->
                HomeModuleCardBean(
                    R.drawable.ic_home_file_management,
                    R.string.home_module_file_management
                ) {
                    startActivity(Intent(this, FileManagementActivity::class.java))
                }
            HOME_MODULE_APPS_LIST ->
                HomeModuleCardBean(R.drawable.ic_home_apps, R.string.home_module_apps) {
                    startActivity(Intent(this, AppsActivity::class.java))
                }
            HOME_MODULE_MAGISK_LIST ->
                HomeModuleCardBean(
                    R.drawable.ic_home_magisk_module,
                    R.string.home_module_magisk_module
                ) {
                    startActivity(Intent(this, MagiskModuleActivity::class.java))
                }
            else -> null
        }
    }

    override fun onResume() {
        AppManager.observeForever(observer)
        super.onResume()
        renewUpdateStatus()
    }

    override fun onPause() {
        AppManager.removeObserver(observer)
        super.onPause()
    }

    private fun renewUpdateStatus() {
        val appMap = AppManager.getAppMap()
        runUiFun {
            val needUpdateNum = appMap[Updater.APP_OUTDATED]?.size ?: 0
            binding.layoutUpdatingCard.tvSubtitle.text =
                String.format(getString(R.string.home_format_items_need_update), needUpdateNum)
            binding.layoutUpdatingCard.tsTitle.setText(getString(R.string.home_check_updates))
            binding.layoutUpdatingCard.ivIcon.setImageResource(R.drawable.ic_done)
        }
    }

    private fun checkUpdate() {
        binding.layoutUpdatingCard.tsTitle.setText(getString(R.string.home_checking_updates))
        binding.layoutUpdatingCard.ivIcon.setImageResource(R.drawable.ic_loading)
        UpdateService.startService(this@MainActivity)
    }

    companion object {
        const val HOME_MODULE_DISCOVERY = "HOME_MODULE_DISCOVERY"
        const val HOME_MODULE_HUB_MANAGER = "HOME_MODULE_HUB_MANAGER"
        const val HOME_MODULE_FILE_MANAGER = "HOME_MODULE_FILE_MANAGER"
        const val HOME_MODULE_APPS_LIST = "HOME_MODULE_APPS_LIST"
        const val HOME_MODULE_MAGISK_LIST = "HOME_MODULE_MAGISK_LIST"

        fun getBeanName(id: String): Int? {
            return when (id) {
                HOME_MODULE_DISCOVERY -> R.string.home_module_discovery
                HOME_MODULE_HUB_MANAGER -> R.string.app_hub
                HOME_MODULE_FILE_MANAGER -> R.string.home_module_file_management
                HOME_MODULE_APPS_LIST -> R.string.home_module_apps
                HOME_MODULE_MAGISK_LIST -> R.string.home_module_magisk_module
                else -> null
            }
        }
    }
}
