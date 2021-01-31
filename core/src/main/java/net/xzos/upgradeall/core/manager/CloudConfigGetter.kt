package net.xzos.upgradeall.core.manager

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.data.json.*
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.module.network.DataCache
import net.xzos.upgradeall.core.module.network.GrpcApi
import net.xzos.upgradeall.core.module.network.OkHttpApi


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

    suspend fun renew() {
        cloudConfig = DataCache.getAnyCache(CLOUD_CONFIG_CACHE_KEY)
                ?: getCloudConfigFromWeb(appCloudRulesHubUrl)?.also {
                    DataCache.cacheAny(CLOUD_CONFIG_CACHE_KEY, it)
                }
    }

    val appConfigList: List<AppConfigGson>?
        get() = cloudConfig?.appList

    val hubConfigList: List<HubConfigGson>?
        get() = cloudConfig?.hubList

    private suspend fun getCloudConfigFromWeb(url: String?): CloudConfigList? {
        val jsonText = if (url != null)
            @Suppress("BlockingMethodInNonBlockingContext")
            OkHttpApi.get(objectTag, url)?.body?.string()
        else GrpcApi.getCloudConfig()
        return if (!jsonText.isNullOrEmpty()) {
            try {
                Gson().fromJson(jsonText, CloudConfigList::class.java)
            } catch (e: JsonSyntaxException) {
                Log.e(objectTag, TAG, "refreshData: ERROR_MESSAGE: $e")
                null
            }
        } else null
    }


    private fun getAppCloudConfig(appUuid: String?): AppConfigGson? {
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
    private suspend fun downloadCloudHubConfig(hubUuid: String?, notifyFun: (Int) -> Unit): Boolean {
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
        return false
    }

    /**
     * 添加数据库成功, NULL 添加数据库失败
     * @return AppDatabase
     */
    suspend fun downloadCloudAppConfig(appUuid: String?, notifyFun: (Int) -> Unit): AppEntity? {
        getAppCloudConfig(appUuid)?.run {
            notifyFun(SUCCESS_GET_APP_DATA)
            if (solveHubDependency(this.baseHubUuid, notifyFun)) {
                this.toAppEntity()?.run {
                    // 添加数据库
                    val appDatabase = AppManager.updateApp(this)
                    if (appDatabase != null) {
                        notifyFun(SUCCESS_SAVE_APP_DATA)
                        notifyFun(SUCCESS)
                        return appDatabase
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
        return if (HubManager.getHub(hubUuid) == null) {
            downloadCloudHubConfig(hubUuid, notifyFun)
        } else true
    }

    suspend fun renewAllAppConfigFromCloud() {
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
        val hubConfigList = hubConfigList ?: return
        val hubDao = metaDatabase.hubDao()
        for (hubConfig in hubConfigList) {
            val hubUuid = hubConfig.uuid
            val hubDatabase = hubDao.loadByUuid(hubUuid) ?: return
            val cloudHubVersion = hubConfig.configVersion
            val localHubVersion = hubDatabase.hubConfig.configVersion
            if (cloudHubVersion > localHubVersion)
                downloadCloudHubConfig(hubUuid) {}
        }
    }
}