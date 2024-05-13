package net.xzos.upgradeall.core.manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.database.dao.HubDao
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.database.table.setSortHubUuidList
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.AutoTemplate
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.websdk.getServerApi
import net.xzos.upgradeall.websdk.data.json.AppConfigGson
import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.HubConfigGson


object CloudConfigGetter {
    private const val TAG = "CloudConfigGetter"
    private val objectTag = ObjectTag(core, TAG)

    private val appCloudRulesHubUrl: String? get() = coreConfig.cloud_rules_hub_url
    private var cloudConfig: CloudConfigList? = null
    private val renewMutex = Mutex()

    suspend fun renew() {
        withContext(Dispatchers.IO) {
            renewMutex.withLock {
                cloudConfig = getCloudConfigFromWeb(appCloudRulesHubUrl) ?: cloudConfig
            }
        }
    }

    val appConfigList: List<AppConfigGson>?
        get() = cloudConfig?.appList

    val hubConfigList: List<HubConfigGson>?
        get() = cloudConfig?.hubList

    private fun getCloudConfigFromWeb(url: String?): CloudConfigList? {
        val key = if (url.isNullOrBlank())
            "http://${coreConfig.update_server_url}/v1/rules/download/dev"
        else url
        return getServerApi().getCloudConfig(key)
    }

    fun getAppCloudConfig(appUuid: String?): AppConfigGson? {
        val appConfigList = this.appConfigList ?: return null
        for (appConfigGson in appConfigList) {
            if (appConfigGson.uuid == appUuid)
                return appConfigGson
        }
        return null
    }

    fun getHubCloudConfig(hubUuid: String?): HubConfigGson? {
        val hubConfigList = this.hubConfigList ?: return null
        for (hubConfigGson in hubConfigList) {
            if (hubConfigGson.uuid == hubUuid)
                return hubConfigGson
        }
        return null
    }

    /**
     * 下载软件源云配置
     * @see GetStatus.SUCCESS_GET_HUB_DATA 获取 HubConfig 成功
     * @see GetStatus.SUCCESS 添加数据库成功
     * @see GetStatus.FAILED_GET_HUB_DATA 获取 HubConfig 失败
     * @see GetStatus.FAILED 添加数据库失败
     */
    suspend fun downloadCloudHubConfig(hubUuid: String?, notifyFun: (GetStatus) -> Unit): Boolean {
        getHubCloudConfig(hubUuid)?.run {
            notifyFun(GetStatus.SUCCESS_GET_HUB_DATA)
            if (HubManager.updateHub(this.toHubEntity())) {
                notifyFun(GetStatus.SUCCESS_SAVE_HUB_DATA)
                notifyFun(GetStatus.SUCCESS)
                return true
            } else {
                notifyFun(GetStatus.FAILED_SAVE_HUB_DATA)
                notifyFun(GetStatus.FAILED)
            }
        } ?: notifyFun(GetStatus.FAILED_GET_HUB_DATA)
        HubManager.checkInvalidApplications()
        return false
    }

    /**
     * 返回 App 添加数据库成功, NULL 添加数据库失败
     * @return App
     */
    suspend fun downloadCloudAppConfig(appUuid: String?, notifyFun: (GetStatus) -> Unit): App? {
        getAppCloudConfig(appUuid)?.also {
            notifyFun(GetStatus.SUCCESS_GET_APP_DATA)
            if (solveHubDependency(it.baseHubUuid, notifyFun)) {
                it.toAppEntity()?.apply {
                    // 添加数据库
                    val app = AppManager.saveApp(this)
                    if (app != null) {
                        notifyFun(GetStatus.SUCCESS_SAVE_APP_DATA)
                        notifyFun(GetStatus.SUCCESS)
                        return app
                    } else {
                        notifyFun(GetStatus.FAILED_SAVE_APP_DATA)
                        notifyFun(GetStatus.FAILED)
                    }
                } ?: notifyFun(GetStatus.FAILED_GET_APP_DATA)
            } else notifyFun(GetStatus.FAILED_GET_HUB_DATA)
        } ?: notifyFun(GetStatus.FAILED_GET_APP_DATA)
        return null
    }

    private suspend fun solveHubDependency(
        hubUuid: String, notifyFun: (GetStatus) -> Unit
    ): Boolean {
        return if (HubManager.getHub(hubUuid) == null)
            downloadCloudHubConfig(hubUuid, notifyFun)
        else
            renewHubConfig(hubUuid)
    }

    suspend fun renewAllAppConfigFromCloud() {
        renew()
        val appConfigList = appConfigList ?: return
        val appDatabaseMap = metaDatabase.appDao().loadAll()
            .filter { it.cloudConfig != null }
            .associateBy({ it.cloudConfig!!.uuid }, { it })
        for (appConfig in appConfigList) {
            val appUuid = appConfig.uuid
            val appDatabase = appDatabaseMap[appUuid] ?: return
            val cloudAppVersion = appConfig.configVersion
            val localAppVersion = appDatabase.cloudConfig!!.configVersion
            if (cloudAppVersion > localAppVersion)
                downloadCloudAppConfig(appUuid) {}
        }
    }

    suspend fun renewAllHubConfigFromCloud() {
        renew()
        val hubDao = metaDatabase.hubDao()
        hubConfigList?.forEach {
            renewHubConfig(it.uuid, hubDao)
        }
    }

    private suspend fun renewHubConfig(
        hubUuid: String,
        hubDao: HubDao = metaDatabase.hubDao()
    ): Boolean {
        val hubDatabase = hubDao.loadByUuid(hubUuid) ?: return false
        val cloudHubVersion = getHubCloudConfig(hubUuid)?.configVersion ?: return false
        val localHubVersion = hubDatabase.hubConfig.configVersion
        return if (cloudHubVersion > localHubVersion)
            downloadCloudHubConfig(hubUuid) {}
        else true
    }
}

enum class GetStatus(val value: Int) {
    SUCCESS(1),
    SUCCESS_GET_APP_DATA(2),
    SUCCESS_GET_HUB_DATA(3),
    SUCCESS_SAVE_APP_DATA(4),
    SUCCESS_SAVE_HUB_DATA(5),

    FAILED(-1),
    FAILED_GET_APP_DATA(-2),
    FAILED_GET_HUB_DATA(-3),
    FAILED_SAVE_APP_DATA(-4),
    FAILED_SAVE_HUB_DATA(-5);
}

suspend fun HubConfigGson.toHubEntity(): HubEntity {
    return metaDatabase.hubDao().loadByUuid(this.uuid)?.also {
        it.hubConfig = this
    } ?: HubEntity(this.uuid, this, mutableMapOf())
}


fun AppConfigGson.getAppId(): Map<String, String>? {
    val hubConfig = CloudConfigGetter.getHubCloudConfig(baseHubUuid) ?: return null
    return info.extraMap.plus(
        AutoTemplate.urlToAppId(info.url, hubConfig.appUrlTemplates) ?: mapOf()
    )
}

suspend fun AppConfigGson.toAppEntity(): AppEntity? {
    val appId = this.getAppId() ?: return null
    val appDatabaseList = metaDatabase.appDao().loadAll()
    val appUuid = uuid
    for (appDatabase in appDatabaseList) {
        if (appDatabase.cloudConfig?.uuid == appUuid) {
            appDatabase.name = info.name
            appDatabase.cloudConfig = this
            val baseHubUuid = (appDatabase.getSortHubUuidList() + baseHubUuid).toSet()
            appDatabase.setSortHubUuidList(baseHubUuid)
            return appDatabase
        }
    }
    return AppEntity(info.name, appId, cloudConfig = this).apply {
        setSortHubUuidList(listOf(baseHubUuid))
    }
}