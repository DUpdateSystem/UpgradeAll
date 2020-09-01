package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_APP_PACKAGE
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_MAGISK_MODULE
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_SHELL
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_SHELL_ROOT
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.data.gson.UIConfig
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardViewExtraData
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.utils.setValueBackground

abstract class AppListContainerViewModel : ViewModel() {
    private val appListLiveData: MutableLiveData<List<BaseApp>> = mutableLiveDataOf()  // 列表中所有的 APP

    internal fun setAppList(list: List<BaseApp>) {
        appListLiveData.setValueBackground(list)
    }

    internal val needUpdateAppsLiveData: MutableLiveData<MutableList<BaseApp>> = mutableLiveDataOf()  // 需要升级的 APP
    private val context = MyApplication.context

    // 列表中所有的 APP 项的信息
    internal val appCardViewList: LiveData<MutableList<ItemCardView>> by lazy {
        Transformations.map(appListLiveData) { apps ->
            return@map apps.map {
                it.getAppItemCardView()
            }.toMutableList().apply {
                if (this.isNotEmpty())
                    this.add(ItemCardView())
            }
        }
    }

    internal fun getAppList(): List<BaseApp> = appListLiveData.value!!

    private fun BaseApp.getAppItemCardView(): ItemCardView {
        val hubName = HubDatabaseManager.getDatabase(appDatabase.hubUuid)?.hubConfig?.info?.hubName
        val local = AppValue.locale
        val type = when (this) {
            is App -> {
                val appDatabase = this.appDatabase
                when (appDatabase.packageId?.api?.toLowerCase(local)) {
                    API_TYPE_APP_PACKAGE -> context.getString(R.string.android_app)
                    API_TYPE_MAGISK_MODULE -> context.getString(R.string.magisk_module)
                    API_TYPE_SHELL -> context.getString(R.string.shell)
                    API_TYPE_SHELL_ROOT -> context.getString(R.string.shell_root)
                    else -> context.getString(R.string.app)
                }
            }
            is Applications -> context.getString(R.string.applications)
            else -> ""
        }
        return ItemCardView(
                appDatabase.name,
                type,
                hubName,
                ItemCardViewExtraData(app = this)
        )
    }

    fun getTabIndexList(): List<Pair<Int, UIConfig.CustomContainerTabListBean>> {
        return mutableListOf(Pair(AppTabSectionsPagerAdapter.USER_STAR_PAGE_INDEX, UIConfig.uiConfig.userStarTab)).apply {
            for ((index, customContainerTabListBean) in UIConfig.uiConfig.userTabList.withIndex()) {
                add(Pair(index, customContainerTabListBean))
            }
        }
    }
}
