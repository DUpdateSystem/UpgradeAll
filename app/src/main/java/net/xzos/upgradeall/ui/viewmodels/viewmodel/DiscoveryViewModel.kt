package net.xzos.upgradeall.ui.viewmodels.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_CUSTOM_SHELL
import net.xzos.upgradeall.core.data.ANDROID_CUSTOM_SHELL_ROOT
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.data.json.AppConfigGson
import net.xzos.upgradeall.core.data.json.getAppId
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.utils.getPackageId
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView
import net.xzos.upgradeall.utils.ToastUtil

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    val cloudApplications: MutableLiveData<List<CloudConfigListItemView>> = MutableLiveData()

    fun requestCloudApplications() = viewModelScope.launch(Dispatchers.IO) {
        CloudConfigGetter.renew()

        withContext(Dispatchers.Main) {
            cloudApplications.value = CloudConfigGetter.appConfigList?.mapNotNull { getCloudAppItemCardView(it) }
        }
    }

    fun downloadApplicationData(uuid: String) {
        ToastUtil.makeText(R.string.download_start, Toast.LENGTH_LONG)
        // 下载数据
        viewModelScope.launch(Dispatchers.IO) {
            // 下载数据
            CloudConfigGetter.downloadCloudAppConfig(uuid) {}
        }
    }

    private fun getCloudAppItemCardView(appConfig: AppConfigGson): CloudConfigListItemView? {
        val name = appConfig.info.name
        val appUuid = appConfig.uuid
        val appCloudConfig = CloudConfigGetter.getAppCloudConfig(appUuid)
        val type: Int = when (appCloudConfig?.getAppId()?.getPackageId()?.first) {
            ANDROID_APP_TYPE -> R.string.android_app
            ANDROID_MAGISK_MODULE_TYPE -> R.string.magisk_module
            ANDROID_CUSTOM_SHELL -> R.string.shell
            ANDROID_CUSTOM_SHELL_ROOT -> R.string.shell_root
            else -> return null
        }
        val hubUuid = appCloudConfig.baseHubUuid
        val hubName = CloudConfigGetter.getHubCloudConfig(hubUuid)?.info?.hubName ?: return null
        return CloudConfigListItemView(name, type, hubName, appUuid)
    }
}