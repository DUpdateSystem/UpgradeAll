package net.xzos.upgradeall.ui.home

import android.content.Intent
import android.os.Bundle
import com.absinthe.libraries.utils.extensions.addPaddingBottom
import com.absinthe.libraries.utils.extensions.addPaddingTop
import com.absinthe.libraries.utils.utils.UiUtils
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.core.utils.FuncR
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.databinding.ActivityMainBinding
import net.xzos.upgradeall.server.update.UpdateService
import net.xzos.upgradeall.ui.activity.LogActivity
import net.xzos.upgradeall.ui.activity.SettingsActivity
import net.xzos.upgradeall.ui.apphub.apps.AppsActivity
import net.xzos.upgradeall.ui.apphub.discover.DiscoverActivity
import net.xzos.upgradeall.ui.apphub.filemanagement.FileManagementActivity
import net.xzos.upgradeall.ui.apphub.magisk.MagiskModuleActivity
import net.xzos.upgradeall.ui.base.BaseActivity
import net.xzos.upgradeall.ui.home.adapter.HomeModuleAdapter
import net.xzos.upgradeall.ui.home.adapter.HomeModuleCardBean
import net.xzos.upgradeall.ui.home.adapter.HomeModuleNonCardBean
import net.xzos.upgradeall.ui.others.OthersActivity
import net.xzos.upgradeall.ui.rss.RssActivity
import net.xzos.upgradeall.utils.ToastUtil
import net.xzos.upgradeall.utils.UxUtils
import net.xzos.upgradeall.utils.runUiFun

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()

        checkUpdate()
        PreferencesMap.initByActivity(this)
    }


    private fun initView() {
        val homeAdapter = HomeModuleAdapter()
        binding.apply {
            rvModules.apply {
                adapter = homeAdapter
                setHasFixedSize(true)
                addPaddingBottom(UiUtils.getNavBarHeight(contentResolver))
            }
            layoutTitleBar.root.addPaddingTop(UxUtils.getStatusBarHeight(resources))
        }
        val moduleList = listOf(
                HomeModuleCardBean(R.drawable.ic_home_discovery, R.string.home_module_discovery) {
                    startActivity(Intent(this, DiscoverActivity::class.java))
                },
                HomeModuleCardBean(R.drawable.ic_home_file_management, R.string.home_module_file_management) {
                    startActivity(Intent(this, FileManagementActivity::class.java))
                },
                HomeModuleCardBean(R.drawable.ic_home_apps, R.string.home_module_apps) {
                    startActivity(Intent(this, AppsActivity::class.java))
                },
                HomeModuleCardBean(R.drawable.ic_home_magisk_module, R.string.home_module_magisk_module) {
                    startActivity(Intent(this, MagiskModuleActivity::class.java))
                },
                HomeModuleCardBean(R.drawable.ic_home_rss, R.string.home_module_rss) {
                    startActivity(Intent(this, RssActivity::class.java))
                },
                HomeModuleCardBean(R.drawable.ic_home_others, R.string.home_module_others) {
                    startActivity(Intent(this, OthersActivity::class.java))
                },
                HomeModuleNonCardBean(R.drawable.ic_home_log, R.string.home_log) {
                    startActivity(Intent(this, LogActivity::class.java))
                },
                HomeModuleNonCardBean(R.drawable.ic_home_setting, R.string.home_settings) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                },
                HomeModuleNonCardBean(R.drawable.ic_home_about, R.string.home_about) {
                    ToastUtil.makeText(R.string.home_about)
                },
        )
        homeAdapter.setList(moduleList)
        binding.layoutUpdatingCard.apply {
            layoutCard.setOnClickListener {
                checkUpdate()
            }
        }

        AppManager.appMapStatusChangedFun = FuncR {
            runUiFun {
                val needUpdateNum = it[Updater.APP_OUTDATED]
                binding.layoutUpdatingCard.tvSubtitle.text = String.format(getString(R.string.home_format_items_need_update), needUpdateNum)
                binding.layoutUpdatingCard.tsTitle.setText(getString(R.string.home_check_updates))
                binding.layoutUpdatingCard.ivIcon.setImageResource(R.drawable.ic_done)
            }
        }
    }

    private fun checkUpdate() {
        binding.layoutUpdatingCard.tsTitle.setText(getString(R.string.home_checking_updates))
        binding.layoutUpdatingCard.ivIcon.setImageResource(R.drawable.ic_loading)
        UpdateService.startService(this@MainActivity)
    }
}
