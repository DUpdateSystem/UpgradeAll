package net.xzos.upgradeall.ui.home

import android.os.Bundle
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.databinding.ActivityMainBinding
import net.xzos.upgradeall.server.update.UpdateService
import net.xzos.upgradeall.ui.activity.BaseActivity
import net.xzos.upgradeall.ui.home.adapter.HomeModuleAdapter
import net.xzos.upgradeall.ui.home.adapter.HomeModuleCardBean
import net.xzos.upgradeall.ui.home.adapter.HomeModuleNonCardBean
import net.xzos.upgradeall.utils.ToastUtil
import net.xzos.upgradeall.utils.egg

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()

        UpdateService.startService(this)
        egg()
        PreferencesMap.initByActivity(this)
    }

    private fun initView() {
        val homeAdapter = HomeModuleAdapter()
        binding.apply {
            rvModules.apply {
                adapter = homeAdapter
            }
        }
        val moduleList = listOf(
                HomeModuleCardBean(R.drawable.ic_home_file_management, R.string.home_module_file_management) {
                    ToastUtil.makeText(R.string.home_module_file_management)
                },
                HomeModuleCardBean(R.drawable.ic_home_apps, R.string.home_module_apps) {
                    ToastUtil.makeText(R.string.home_module_apps)
                },
                HomeModuleCardBean(R.drawable.ic_home_magisk_module, R.string.home_module_magisk_module) {
                    ToastUtil.makeText(R.string.home_module_magisk_module)
                },
                HomeModuleCardBean(R.drawable.ic_home_rss, R.string.home_module_rss) {
                    ToastUtil.makeText(R.string.home_module_rss)
                },
                HomeModuleCardBean(R.drawable.ic_home_others, R.string.home_module_others) {
                    ToastUtil.makeText(R.string.home_module_others)
                },
                HomeModuleNonCardBean(R.drawable.ic_home_log, R.string.home_log) {
                    ToastUtil.makeText(R.string.home_log)
                },
                HomeModuleNonCardBean(R.drawable.ic_home_setting, R.string.home_settings) {
                    ToastUtil.makeText(R.string.home_settings)
                },
                HomeModuleNonCardBean(R.drawable.ic_home_about, R.string.home_about) {
                    ToastUtil.makeText(R.string.home_about)
                },
        )
        homeAdapter.setList(moduleList)
    }
}
