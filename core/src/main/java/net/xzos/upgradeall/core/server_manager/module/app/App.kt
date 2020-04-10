package net.xzos.upgradeall.core.server_manager.module.app

import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.core.data_manager.utils.AutoTemplate
import net.xzos.upgradeall.core.route.AppIdItem
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.system_api.api.IoApi

class App(database: AppDatabase) : BaseApp(database) {

    val hubDatabase = HubDatabaseManager.getDatabase(appDatabase.hubUuid)
    var appId: List<AppIdItem>? = null
        get() {
            if (field != null) return field
            if (hubDatabase != null)
                for (appUrlTemplates in hubDatabase.hubConfig.appUrlTemplates) {
                    val args = AutoTemplate(appDatabase.url, appUrlTemplates).args.map {
                        AutoTemplate.Arg(it.key.substringAfterLast("%"), it.value)
                    }
                    val getKeys = args.map {
                        it.key
                    }
                    if (getKeys == hubDatabase.hubConfig.apiKeywords)
                        return args.map {
                            AppIdItem.newBuilder().setKey(it.key).setValue(it.value).build()
                        }.also {
                            field = it
                        }
                }
            return null
        }

    val markProcessedVersionNumber: String?
        get() = this.appDatabase.extraData?.markProcessedVersionNumber

    override suspend fun getUpdateStatus(): Int {
        return Updater(this).getUpdateStatus()
    }

    // 获取已安装版本号
    val installedVersionNumber: String?
        get() = IoApi.getAppVersionNumber(this.appDatabase.targetChecker)
}
