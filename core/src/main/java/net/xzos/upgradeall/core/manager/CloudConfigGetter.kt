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
    private const val SUCCESS_GET_DATA = 2
    private const val FAILED_GET_DATA = -2

    private const val CLOUD_CONFIG_CACHE_KEY = "CLOUD_CONFIG"
    private val appCloudRulesHubUrl: String get() = coreConfig.cloud_rules_hub_url
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
     * @return
     * @see SUCCESS_GET_DATA 获取 HubConfig 成功
     * @see SUCCESS 添加数据库成功
     * @see FAILED_GET_DATA 获取 HubConfig 失败
     * @see FAILED 添加数据库失败
     */
    suspend fun downloadCloudHubConfig(hubUuid: String?): Int {
        val cloudHubConfigGson = getHubCloudConfig(hubUuid)
        return if (cloudHubConfigGson != null) {
            if (HubManager.updateHub(cloudHubConfigGson.toHubEntity()))
                SUCCESS
            else FAILED
        } else FAILED_GET_DATA
    }

    /**
     * 添加数据库成功, NULL 添加数据库失败
     * @return AppDatabase
     */
    suspend fun downloadCloudAppConfig(appUuid: String?): AppEntity? {
        val appConfigGson = getAppCloudConfig(appUuid)
        if (appConfigGson != null) {
            // 添加数据库
            val appDatabase = AppManager.updateApp(appConfigGson.toAppEntity())
            if (appDatabase != null) {
                Log.i(objectTag, TAG, "数据添加成功")
                return appDatabase
            } else
                Log.e(objectTag, TAG, "什么？数据库添加失败！")
        } else
            Log.e(objectTag, TAG, "获取基础配置文件失败")
        return null
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
                downloadCloudAppConfig(appUuid)
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
                downloadCloudHubConfig(hubUuid)
        }
    }
}