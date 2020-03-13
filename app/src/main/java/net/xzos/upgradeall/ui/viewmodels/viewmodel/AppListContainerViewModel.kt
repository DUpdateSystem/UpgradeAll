package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.dupdatesystem.core.data.config.AppConfig
import net.xzos.dupdatesystem.core.data.database.AppDatabase
import net.xzos.dupdatesystem.core.data.json.gson.AppConfigGson
import net.xzos.dupdatesystem.core.data_manager.HubDatabaseManager
import net.xzos.dupdatesystem.core.server_manager.module.BaseApp
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardViewExtraData

abstract class AppListContainerViewModel : ViewModel() {
    internal val needUpdateAppsLiveData = MutableLiveData(mutableListOf<BaseApp>())  // 需要升级的 APP
    private val appListLiveData = MutableLiveData(mutableListOf<BaseApp>())  // 列表中所有的 APP
    private val context = MyApplication.context
    var dataInit = false

    fun setApps(apps: List<BaseApp>) {
        GlobalScope.launch(Dispatchers.Main) {
            dataInit = true
            appListLiveData.value = apps.toMutableList()
        }
    }

    // 列表中所有的 APP 项的信息
    internal val appCardViewList = Transformations.map(appListLiveData) { apps ->
        return@map mutableListOf<ItemCardView>().apply {
            for (app in apps) {
                this.add(getAppItemCardView(app))
            }
            if (apps.isNotEmpty()) {
                this.add(ItemCardView())
            }
        }
    }

    private fun getAppItemCardView(app: BaseApp): ItemCardView {
        val appInfo = app.appInfo
        val hubName = HubDatabaseManager.getDatabase(appInfo.apiUuid)?.name ?: ""
        val local = AppConfig.locale
        val type = when (appInfo.type) {
            AppDatabase.APP_TYPE_TAG -> {
                when (appInfo.targetChecker?.api?.toLowerCase(local)) {
                    AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_APP_PACKAGE -> context.getString(R.string.android_app)
                    AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_MAGISK_MODULE -> context.getString(R.string.magisk_module)
                    AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_SHELL -> context.getString(R.string.shell)
                    AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_SHELL_ROOT -> context.getString(R.string.shell_root)
                    else -> ""
                }
            }
            AppDatabase.APPLICATIONS_TYPE_TAG.toLowerCase(local) -> context.getString(R.string.applications)
            else -> ""
        }
        return ItemCardView(
                appInfo.name,
                type,
                hubName,
                ItemCardViewExtraData(app = app)
        )
    }
}
