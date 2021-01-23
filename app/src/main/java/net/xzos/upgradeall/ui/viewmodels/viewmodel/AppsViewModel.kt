package net.xzos.upgradeall.ui.viewmodels.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import net.xzos.upgradeall.core.data_manager.CloudConfigGetter
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.server.update.UpdateService
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView
import net.xzos.upgradeall.utils.ToastUtil

class AppsViewModel(private val _application: Application) : AndroidViewModel(_application) {

    val cloudApplications: MutableLiveData<List<CloudConfigListItemView>> = MutableLiveData()

    fun requestCloudApplications() = viewModelScope.launch(Dispatchers.IO) {
        UpdateService.startService(_application)

        withContext(Dispatchers.Main) {
            cloudApplications.value = CloudConfigGetter.appConfigList?.map { getCloudAppItemCardView(it) }
        }
    }

    fun downloadApplicationData(context: Context, uuid: String) {
        ToastUtil.makeText(R.string.download_start, Toast.LENGTH_LONG)
        // 下载数据
        viewModelScope.launch(Dispatchers.IO) {
            // 下载数据
            val appDatabase = CloudConfigGetter.downloadCloudAppConfig(uuid)

            withContext(Dispatchers.Main) {
                if (appDatabase != null) {
                    checkHubDependency(context, hubUuid = appDatabase.hubUuid)
                }
            }
        }
    }

    private fun getCloudAppItemCardView(appConfig: AppConfigGson): CloudConfigListItemView {
        val name = appConfig.info.appName
        val appUuid = appConfig.uuid
        val appCloudConfig = CloudConfigGetter.getAppCloudConfig(appUuid)
        val type: Int? = when (appCloudConfig?.appConfig?.targetChecker?.api?.toLowerCase(AppValue.locale)) {
            PackageIdGson.API_TYPE_APP_PACKAGE -> R.string.android_app
            PackageIdGson.API_TYPE_MAGISK_MODULE -> R.string.magisk_module
            PackageIdGson.API_TYPE_SHELL -> R.string.shell
            PackageIdGson.API_TYPE_SHELL_ROOT -> R.string.shell_root
            else -> null
        }
        val hubUuid = appCloudConfig?.appConfig?.hubInfo?.hubUuid
        val hubName = CloudConfigGetter.getHubCloudConfig(hubUuid)?.info?.hubName
        return CloudConfigListItemView(name, type, hubName, appUuid)
    }

    private fun checkHubDependency(context: Context, hubUuid: String?) {
        if (!HubDatabaseManager.exists(hubUuid)) {
            AlertDialog.Builder(context).apply {
                setMessage(R.string.whether_download_dependency_hub)
                setPositiveButton(android.R.string.ok) { dialog, _ ->
                    ToastUtil.makeText(R.string.start_download_dependency_hub, Toast.LENGTH_LONG)
                    GlobalScope.launch { CloudConfigGetter.downloadCloudHubConfig(hubUuid) }
                    dialog.cancel()
                }
                setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                setCancelable(false)
            }.create().show()
        }
    }
}