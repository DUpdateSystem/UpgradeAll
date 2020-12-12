package net.xzos.upgradeall.ui.viewmodels.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import net.xzos.upgradeall.core.data_manager.CloudConfigGetter
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    val cloudApplications: MutableLiveData<List<CloudConfigListItemView>> = MutableLiveData()

    fun requestCloudApplications() = viewModelScope.launch(Dispatchers.IO) {
        CloudConfigGetter.renew()

        withContext(Dispatchers.Main) {
            cloudApplications.value = CloudConfigGetter.appConfigList?.map { getCloudAppItemCardView(it) }
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
}