package net.xzos.upgradeall.core.data_manager

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.CloudConfigList
import net.xzos.upgradeall.core.data.json.gson.HubConfigGson
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag.Companion.core
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.network.OkHttpApi
import net.xzos.upgradeall.core.network.DataCache
import net.xzos.upgradeall.core.network.ServerApi


object CloudConfigGetter {
    private const val TAG = "CloudConfigGetter"
    private val objectTag = ObjectTag(core, TAG)

    private const val SUCCESS = 1
    private const val FAILED = -1
    private const val SUCCESS_GET_DATA = 2
    private const val FAILED_GET_DATA = -2

    private const val CLOUD_CONFIG_CACHE_KEY = "CLOUD_CONFIG"
    private val appCloudRulesHubUrl: String? get() = AppConfig.app_cloud_rules_hub_url
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
            OkHttpApi.getWithoutError(objectTag, url)?.body?.string()
        else ServerApi.getCloudConfig()
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
            if (HubDatabaseManager.addDatabase(cloudHubConfigGson))
                SUCCESS
            else FAILED
        } else FAILED_GET_DATA
    }

    /**
     * 添加数据库成功, NULL 添加数据库失败
     * @return AppDatabase
     */
    suspend fun downloadCloudAppConfig(appUuid: String?): AppDatabase? {
        val appConfigGson = getAppCloudConfig(appUuid)
        if (appConfigGson != null) {
            // 添加数据库
            val appDatabase =
                    AppDatabaseManager.saveAppConfig(appConfigGson)
            if (appDatabase != null) {
                Log.i(objectTag, TAG, "数据添加成功")
                return appDatabase
            } else
                Log.e(objectTag, TAG, "什么？数据库添加失败！")
        } else
            Log.e(objectTag, TAG, "获取基础配置文件失败")
        return null
    }
}

suspend fun AppDatabaseManager.renewAllAppConfigFromCloud() {
    val appConfigList = CloudConfigGetter.appConfigList ?: return
    for (appConfig in appConfigList) {
        val appUuid = appConfig.uuid
        val appDatabase = getAppDatabase(uuid = appUuid)
        if (appDatabase != null) {
            val cloudAppVersion = appConfig.info.configVersion
            val localAppVersion = appDatabase.cloudConfig?.info?.configVersion ?: 0
            if (cloudAppVersion > localAppVersion)
                CloudConfigGetter.downloadCloudAppConfig(appUuid)
        }
    }
}

suspend fun HubDatabaseManager.renewAllHubConfigFromCloud() {
    val hubConfigList = CloudConfigGetter.hubConfigList ?: return
    for (hubConfig in hubConfigList) {
        val hubUuid = hubConfig.uuid
        val hubDatabase = getDatabase(hubUuid)
        if (hubDatabase != null) {
            val cloudHubVersion = hubConfig.info.configVersion
            val localHubVersion = hubDatabase.hubConfig.info.configVersion
            if (cloudHubVersion > localHubVersion)
                CloudConfigGetter.downloadCloudHubConfig(hubUuid)
        }
    }
}
