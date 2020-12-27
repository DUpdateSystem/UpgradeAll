package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.data.AppUiDataManager
import net.xzos.upgradeall.data.gson.toItemListBean
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardViewExtraData


class AppHubViewModel : AppListContainerViewModel() {

    val itemCountLiveData: MutableLiveData<Int> = MutableLiveData(0)

    private val mTabPageIndex = MutableLiveData<Int>().apply {
        this.observeForever { tabPageIndex ->
            AppUiDataManager.getAppListLivaData(tabPageIndex).observeForever {
                setAppList(it)
            }
        }
    }

    internal fun setTabPageIndex(tabPageIndex: Int) {
        mTabPageIndex.value = tabPageIndex
    }

    fun getTabPageIndex(): Int = mTabPageIndex.value ?: 0

    fun removeItemFromTabPage(position: Int): Boolean {
        return AppUiDataManager.removeItemFromTabPage(position, mTabPageIndex.value!!)
    }

    fun moveItemToOtherTabPage(position: Int, tabPageIndex: Int): Boolean {
        return if (mTabPageIndex.value == AppTabSectionsPagerAdapter.ALL_APP_PAGE_INDEX) {
            AppUiDataManager.addItem(getAppList()[position].appDatabase.toItemListBean(), tabPageIndex)
        } else {
            AppUiDataManager.moveItemToOtherGroup(position, mTabPageIndex.value!!, tabPageIndex).also {
                if (it) removeItemFromTabPage(position)
            }
        }
    }

    private fun BaseApp.getAppItemCardView(): ItemCardView {
        val context = MyApplication.context
        val hubName = HubDatabaseManager.getDatabase(appDatabase.hubUuid)?.hubConfig?.info?.hubName
        val local = AppValue.locale
        val type = when (this) {
            is App -> {
                val appDatabase = this.appDatabase
                when (appDatabase.packageId?.api?.toLowerCase(local)) {
                    PackageIdGson.API_TYPE_APP_PACKAGE -> context.getString(R.string.android_app)
                    PackageIdGson.API_TYPE_MAGISK_MODULE -> context.getString(R.string.magisk_module)
                    PackageIdGson.API_TYPE_SHELL -> context.getString(R.string.shell)
                    PackageIdGson.API_TYPE_SHELL_ROOT -> context.getString(R.string.shell_root)
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

}