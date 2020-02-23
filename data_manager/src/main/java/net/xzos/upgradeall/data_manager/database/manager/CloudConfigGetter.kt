package net.xzos.upgradeall.data_manager.database.manager

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import net.xzos.upgradeall.data.config.AppConfig
import net.xzos.upgradeall.data.json.gson.AppConfigGson
import net.xzos.upgradeall.data.json.gson.CloudConfig
import net.xzos.upgradeall.data.json.gson.HubConfig
import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.data_manager.database.AppDatabase
import net.xzos.upgradeall.data_manager.utils.FilePathUtils
import net.xzos.upgradeall.data_manager.utils.GitUrlTranslation
import net.xzos.upgradeall.log.Log
import net.xzos.upgradeall.network_api.OkHttpApi


class CloudConfigGetter(private val appCloudRulesHubUrl: String?) {

    val available = cloudHubGitUrlTranslation.testUrl()

    private val cloudHubGitUrlTranslation: GitUrlTranslation
        get() {
            val defaultCloudRulesHubUrl = AppConfig.default_cloud_rules_hub_url
            val cloudRulesHubUrl = appCloudRulesHubUrl
                    ?: defaultCloudRulesHubUrl
            return GitUrlTranslation(cloudRulesHubUrl)
        }

    private val hubListRawUrl: String
        get() {
            return cloudHubGitUrlTranslation.getGitRawUrl("rules/hub/")
        }

    private val rulesListJsonFileRawUrl: String
        get() {
            return cloudHubGitUrlTranslation.getGitRawUrl("rules/rules_list.json")
        }

    private val cloudConfig: CloudConfig?
        get() = renewCloudConfig()

    val appList: List<CloudConfig.AppListBean>?
        get() = cloudConfig?.appList

    val hubList: List<CloudConfig.HubListBean>?
        get() = cloudConfig?.hubList

    private fun renewCloudConfig(): CloudConfig? {
        val jsonText = OkHttpApi.getHttpResponse(objectTag, rulesListJsonFileRawUrl)
        return if (jsonText != null && jsonText.isNotEmpty()) {
            try {
                Gson().fromJson(jsonText, CloudConfig::class.java)
            } catch (e: JsonSyntaxException) {
                Log.e(objectTag, TAG, "refreshData: ERROR_MESSAGE: $e")
                null
            }
        } else null
    }

    fun getAppCloudConfig(appUuid: String?): AppConfigGson? {
        getAppCloudConfigUrl(appUuid)?.let { appCloudConfigUrl ->
            val appConfigString = OkHttpApi.getHttpResponse(
                    objectTag, appCloudConfigUrl
            )
            return try {
                Gson().fromJson(appConfigString, AppConfigGson::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }
        } ?: return null
    }

    fun getHubCloudConfig(hubUuid: String?): HubConfig? {
        getHubCloudConfigUrl(hubUuid)?.let { hubCloudConfigUrl ->
            val hubConfigString = OkHttpApi.getHttpResponse(
                    objectTag, hubCloudConfigUrl
            )
            return try {
                Gson().fromJson(hubConfigString, HubConfig::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }
        } ?: return null
    }

    private fun getAppCloudConfigUrl(appUuid: String?): String? {
        appList?.let {
            for (appItem in it) {
                if (appItem.appConfigUuid == appUuid)
                    return cloudHubGitUrlTranslation.getGitRawUrl("rules/apps/${appItem.appConfigFileName}.json")
            }
        }
        return null
    }

    private fun getHubCloudConfigUrl(hubUuid: String?): String? {
        hubList?.let {
            for (hubItem in it) {
                if (hubItem.hubConfigUuid == hubUuid)
                    return cloudHubGitUrlTranslation.getGitRawUrl("rules/hub/${hubItem.hubConfigFileName}.json")
            }
        }
        return null
    }

    private fun getCloudHubConfigJS(filePath: String): String? {
        val hubConfigJSRawUrl = FilePathUtils.pathTransformRelativeToAbsolute(hubListRawUrl, filePath)
        return OkHttpApi.getHttpResponse(objectTag, hubConfigJSRawUrl)
    }

    /**
     * @return
     * @see SUCCESS_GET_DATA 获取 HubConfig 成功
     * @see SUCCESS_GET_JS 获取 JS 成功
     * @see SUCCESS 添加数据库成功
     * @see FAILED_GET_DATA 获取 HubConfig 失败
     * @see FAILED_GET_JS 解析 JS 失败
     * @see FAILED 添加数据库失败
     */
    fun downloadCloudHubConfig(hubUuid: String?): Int {
        val cloudHubConfigGson = getHubCloudConfig(hubUuid)
        // TODO: 配置文件地址与仓库地址分离
        return if (cloudHubConfigGson != null) {
            val cloudHubConfigJS = getCloudHubConfigJS(cloudHubConfigGson.webCrawler?.filePath
                    ?: "")
            if (cloudHubConfigJS != null) {
                // 添加数据库
                if (HubDatabaseManager.addDatabase(cloudHubConfigGson, cloudHubConfigJS)) {
                    SUCCESS
                } else FAILED
            } else FAILED_GET_JS
        } else FAILED_GET_DATA
    }

    /**
     * 添加数据库成功, NULL 添加数据库失败
     * @return AppDatabase
     */
    fun downloadCloudAppConfig(appUuid: String?): AppDatabase? {
        val appConfigGson = getAppCloudConfig(appUuid)
        if (appConfigGson != null) {
            // 添加数据库
            val appDatabase = AppDatabaseManager.setDatabase(appConfigGson)
            if (appDatabase != null) {
                Log.i(objectTag, TAG, "数据添加成功")
                return appDatabase
            } else
                Log.e(objectTag, TAG, "什么？数据库添加失败！")
        } else
            Log.e(objectTag, TAG, "获取基础配置文件失败")
        return null
    }

    companion object {
        private const val TAG = "CloudConfigGetter"
        private val objectTag = ObjectTag("Core", TAG)

        private const val SUCCESS = 1
        private const val FAILED = -1
        private const val SUCCESS_GET_DATA = 2
        private const val FAILED_GET_DATA = -2
        private const val SUCCESS_GET_JS = 3
        private const val FAILED_GET_JS = -3

    }
}
