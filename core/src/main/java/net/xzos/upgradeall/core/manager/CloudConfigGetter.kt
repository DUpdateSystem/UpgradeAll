package net.xzos.upgradeall.core.manager

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.database.dao.HubDao
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.database.table.setSortHubUuidList
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.serverApi
import net.xzos.upgradeall.core.utils.AutoTemplate
import net.xzos.upgradeall.core.utils.DataCache
import net.xzos.upgradeall.core.utils.coroutines.wait
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.json.*
import net.xzos.upgradeall.core.websdk.openOkHttpApi


object CloudConfigGetter {
    private const val TAG = "CloudConfigGetter"
    private val objectTag = ObjectTag(core, TAG)

    private const val SUCCESS = 1
    private const val FAILED = -1
    private const val SUCCESS_GET_APP_DATA = SUCCESS + 1
    private const val SUCCESS_GET_HUB_DATA = SUCCESS + 2
    private const val SUCCESS_SAVE_APP_DATA = SUCCESS + 3
    private const val SUCCESS_SAVE_HUB_DATA = SUCCESS + 4
    private const val FAILED_GET_APP_DATA = FAILED - 1
    private const val FAILED_GET_HUB_DATA = FAILED - 2
    private const val FAILED_SAVE_APP_DATA = FAILED - 3
    private const val FAILED_SAVE_HUB_DATA = FAILED - 4

    private const val CLOUD_CONFIG_CACHE_KEY = "CLOUD_CONFIG"
    private val appCloudRulesHubUrl: String? get() = coreConfig.cloud_rules_hub_url
    private var cloudConfig: CloudConfigList? = null
    private val renewMutex = Mutex()

    private val dataCache = DataCache(coreConfig.data_expiration_time)

    suspend fun renew() {
        cloudConfig = dataCache.get(CLOUD_CONFIG_CACHE_KEY)
            ?: if (renewMutex.isLocked) {
                renewMutex.wait()
                dataCache.get(CLOUD_CONFIG_CACHE_KEY)
            } else renewMutex.withLock {
                getCloudConfigFromWeb(appCloudRulesHubUrl)?.also {
                    dataCache.cache(CLOUD_CONFIG_CACHE_KEY, it)
                }
            }
    }

    val appConfigList: List<AppConfigGson>?
        get() = cloudConfig?.appList

    val hubConfigList: List<HubConfigGson>?
        get() = cloudConfig?.hubList

    private suspend fun getCloudConfigFromWeb(url: String?): CloudConfigList? {
        return if (url != null)
            @Suppress("BlockingMethodInNonBlockingContext")
            conventCloudConfigList(
                openOkHttpApi.getWithoutError(objectTag, url)?.body?.string() ?: return null
            )
        else serverApi.getCloudConfig()
    }

    private fun conventCloudConfigList(json: String): CloudConfigList? {
        return try {
            Gson().fromJson(json, CloudConfigList::class.java)
        } catch (e: JsonSyntaxException) {
            Log.e(objectTag, TAG, "conventCloudConfigList: e: ${e.msg()}")
            null
        }
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
     * @see SUCCESS_GET_HUB_DATA 获取 HubConfig 成功
     * @see SUCCESS 添加数据库成功
     * @see FAILED_GET_HUB_DATA 获取 HubConfig 失败
     * @see FAILED 添加数据库失败
     */
    suspend fun downloadCloudHubConfig(hubUuid: String?, notifyFun: (Int) -> Unit): Boolean {
        getHubCloudConfig(hubUuid)?.run {
            notifyFun(SUCCESS_GET_HUB_DATA)
            if (HubManager.updateHub(this.toHubEntity())) {
                notifyFun(SUCCESS_SAVE_HUB_DATA)
                notifyFun(SUCCESS)
                return true
            } else {
                notifyFun(FAILED_SAVE_HUB_DATA)
                notifyFun(FAILED)
            }
        } ?: notifyFun(FAILED_GET_HUB_DATA)
        HubManager.checkInvalidApplications()
        return false
    }

    /**
     * 返回 App 添加数据库成功, NULL 添加数据库失败
     * @return App
     */
    suspend fun downloadCloudAppConfig(appUuid: String?, notifyFun: (Int) -> Unit): App? {
        getAppCloudConfig(appUuid)?.run {
            notifyFun(SUCCESS_GET_APP_DATA)
            if (solveHubDependency(this.baseHubUuid, notifyFun)) {
                this.toAppEntity()?.run {
                    // 添加数据库
                    val app = AppManager.updateApp(this)
                    if (app != null) {
                        notifyFun(SUCCESS_SAVE_APP_DATA)
                        notifyFun(SUCCESS)
                        return app
                    } else {
                        notifyFun(FAILED_SAVE_APP_DATA)
                        notifyFun(FAILED)
                    }
                } ?: notifyFun(FAILED_GET_APP_DATA)
            }
        } ?: notifyFun(FAILED_GET_APP_DATA)
        return null
    }

    private suspend fun solveHubDependency(hubUuid: String, notifyFun: (Int) -> Unit): Boolean {
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
    val appDatabaseList = metaDatabase.appDao().loadAll()
    val appId = this.getAppId() ?: return null
    for (appDatabase in appDatabaseList) {
        if (appDatabase.appId == appId) {
            appDatabase.name = info.name
            appDatabase.cloudConfig = this
            val baseHubUuid = (appDatabase.getSortHubUuidList() + baseHubUuid).toSet()
            appDatabase.setSortHubUuidList(baseHubUuid)
            return appDatabase
        }
    }
    return AppEntity(0, info.name, appId, cloudConfig = this).apply {
        setSortHubUuidList(listOf(this@toAppEntity.baseHubUuid))
    }
}