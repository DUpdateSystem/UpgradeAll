package net.xzos.upgradeall.ui.viewmodels.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardViewExtraData

abstract class AppListContainerViewModel : ViewModel() {
    internal val needUpdateAppsLiveData = MutableLiveData(mutableListOf<BaseApp>())  // 需要升级的 APP
    internal lateinit var appListLiveData: MutableLiveData<List<BaseApp>>  // 列表中所有的 APP
    private val context = MyApplication.context
    var dataInit = true

    // 列表中所有的 APP 项的信息
    private var appCardViewList: LiveData<MutableList<ItemCardView>>? = null

    internal fun getAppCardViewList(): LiveData<MutableList<ItemCardView>> {
        return if (appCardViewList != null) {
            appCardViewList!!
        } else Transformations.map(appListLiveData) { apps ->
            val appList: MutableList<App> = mutableListOf<App>().also {
                for (app in apps) {
                    when (app) {
                        is App -> it.add(app)
                        is Applications -> it.addAll(runBlocking { app.getNeedUpdateAppList(block = false) })
                    }
                }
            }
            return@map mutableListOf<ItemCardView>().apply {
                for (app in appList) {
                    this.add(getAppItemCardView(app))
                }
                if (appList.isNotEmpty()) {
                    this.add(ItemCardView())
                }
            }
        }.also {
            appCardViewList = it
        }
    }

    private fun getAppItemCardView(app: BaseApp): ItemCardView {
        val appDatabase = app.appDatabase
        val hubName = HubDatabaseManager.getDatabase(appDatabase.hubUuid)?.hubConfig?.info?.hubName
        val local = AppValue.locale
        val type = when (appDatabase.type) {
            AppDatabase.APP_TYPE_TAG -> {
                when (appDatabase.targetChecker?.api?.toLowerCase(local)) {
                    AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_APP_PACKAGE -> context.getString(R.string.android_app)
                    AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_MAGISK_MODULE -> context.getString(R.string.magisk_module)
                    AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_SHELL -> context.getString(R.string.shell)
                    AppConfigGson.AppConfigBean.TargetCheckerBean.API_TYPE_SHELL_ROOT -> context.getString(R.string.shell_root)
                    else -> context.getString(R.string.app)
                }
            }
            AppDatabase.APPLICATIONS_TYPE_TAG.toLowerCase(local) -> context.getString(R.string.applications)
            else -> ""
        }
        return ItemCardView(
                appDatabase.name,
                type,
                hubName,
                ItemCardViewExtraData(app = app)
        )
    }
}
