package net.xzos.upgradeall.ui.viewmodels.viewmodel

import android.app.Application
import kotlinx.coroutines.Dispatchers
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

class DiscoveryViewModel(application: Application) : ListContainerViewModel<CloudConfigListItemView>(application) {

    override suspend fun doLoadData(): List<CloudConfigListItemView> {
        CloudConfigGetter.renew()
        return withContext(Dispatchers.Main) {
            CloudConfigGetter.appConfigList?.mapNotNull { getCloudAppItemCardView(it) }
        } ?: emptyList()
    }

    companion object {
        private fun getCloudAppItemCardView(appConfig: AppConfigGson): CloudConfigListItemView? {
            val name = appConfig.info.name
            val appUuid = appConfig.uuid
            val appId = appConfig.getAppId()
            val type: Int = when (appId?.getPackageId()?.first) {
                ANDROID_APP_TYPE -> R.string.android_app
                ANDROID_MAGISK_MODULE_TYPE -> R.string.magisk_module
                ANDROID_CUSTOM_SHELL -> R.string.shell
                ANDROID_CUSTOM_SHELL_ROOT -> R.string.shell_root
                else -> return null
            }
            val hubUuid = appConfig.baseHubUuid
            val hubName = CloudConfigGetter.getHubCloudConfig(hubUuid)?.info?.hubName ?: return null
            return CloudConfigListItemView(name, type, hubName, appUuid)
        }
    }
}