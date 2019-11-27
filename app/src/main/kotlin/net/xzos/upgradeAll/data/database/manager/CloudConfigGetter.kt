package net.xzos.upgradeAll.data.database.manager

import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.data.json.gson.AppConfig
import net.xzos.upgradeAll.data.json.gson.CloudConfig
import net.xzos.upgradeAll.data.json.gson.HubConfig
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.js.utils.OkHttpApi
import net.xzos.upgradeAll.utils.FileUtil

object CloudConfigGetter {
    private const val TAG = "CloudConfigGetter"
    private val LogObjectTag = arrayOf("Core", TAG)
    private val Log = ServerContainer.Log

    private val okHttpApi = OkHttpApi(LogObjectTag)

    private val rulesListJsonFileRawUrl: String
        get() = getRawRootUrl(
                PreferenceManager.getDefaultSharedPreferences(MyApplication.context).getString(
                        "cloud_rules_hub_url",
                        MyApplication.context.resources.getString(R.string.default_cloud_rules_hub_url)
                ).apply {
                    val separator = '/'
                    if (this?.last() != separator)
                        this.plus(separator)
                }
        ) + "rules/rules_list.json"

    private val cloudConfig: CloudConfig?
        get() = renewCloudConfig()

    val appList: List<CloudConfig.AppListBean>? = cloudConfig?.appList

    val hubList: List<CloudConfig.HubListBean>? = cloudConfig?.hubList

    private fun renewCloudConfig(): CloudConfig? {
        val jsonText = okHttpApi.getHttpResponse(rulesListJsonFileRawUrl).first
        return if (jsonText != null && jsonText.isNotEmpty()) {
            try {
                Gson().fromJson(jsonText, CloudConfig::class.java)
            } catch (e: JsonSyntaxException) {
                Log.e(LogObjectTag, TAG, "refreshData: ERROR_MESSAGE: $e")
                null
            }
        } else null
    }

    fun getAppCloudConfig(appUuid: String?): AppConfig? {
        getAppCloudConfigUrl(appUuid)?.let { appCloudConfigUrl ->
            val appConfigString = okHttpApi.getHttpResponse(
                    appCloudConfigUrl
            ).first
            return try {
                Gson().fromJson(appConfigString, AppConfig::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }
        } ?: return null
    }

    fun getHubCloudConfig(hubUuid: String?): HubConfig? {
        getHubCloudConfigUrl(hubUuid)?.let { hubCloudConfigUrl ->
            val hubConfigString = okHttpApi.getHttpResponse(
                    hubCloudConfigUrl
            ).first
            return try {
                Gson().fromJson(hubConfigString, HubConfig::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }
        } ?: return null
    }

    private fun getAppCloudConfigUrl(appUuid: String?): String? {
        if (appList != null)
            for (appItem in appList) {
                if (appItem.appConfigUuid == appUuid)
                    return "${cloudConfig?.listUrl?.appListRawUrl}${appItem.appConfigFileName}.json"
            }
        return null
    }

    private fun getHubCloudConfigUrl(hubUuid: String?): String? {
        if (hubList != null)
            for (hubItem in hubList) {
                if (hubItem.hubConfigUuid == hubUuid)
                    return "${cloudConfig?.listUrl?.hubListRawUrl}${hubItem.hubConfigFileName}.json"
            }
        return null
    }

    private fun getCloudHubConfigJS(filePath: String): String? {
        val hubListRawUrl = cloudConfig?.listUrl?.hubListRawUrl ?: return null
        val hubConfigJSRawUrl = FileUtil.pathTransformRelativeToAbsolute(hubListRawUrl, filePath)
        return okHttpApi.getHttpResponse(hubConfigJSRawUrl).first
    }

    private fun getRawRootUrl(gitUrl: String?): String {
        if (gitUrl != null) {
            val list = gitUrl.split("github\\.com".toRegex())[1].split("/".toRegex()).filter { it.isNotEmpty() }
            if (list.size >= 2) {
                val owner = list[0]
                val repo = list[1]
                var branch: String? = null
                if (list.size == 2) {
                    branch = "master"
                    // 分割网址
                } else if (list.size == 4 && list.contains("tree")) {
                    branch = list[3]
                    // 分割网址
                }
                if (branch != null)
                    return "https://raw.githubusercontent.com/$owner/$repo/$branch/"
            }
        }
        return ""
    }

    /**
     * @return 1 获取 HubConfig 成功, 2 获取 JS 成功, 3 添加数据库成功, -1 获取 HubConfig 失败, -2 解析 JS 失败, -3 添加数据库失败
     */
    fun downloadCloudHubConfig(hubUuid: String?): Int {
        val context = MyApplication.context
        GlobalScope.launch(Dispatchers.Main) { Toast.makeText(context, "开始下载", Toast.LENGTH_LONG).show() }  // 下载
        val cloudHubConfigGson = getHubCloudConfig(hubUuid)
        // TODO: 配置文件地址与仓库地址分离
        return (if (cloudHubConfigGson != null) {
            val cloudHubConfigJS = getCloudHubConfigJS(cloudHubConfigGson.webCrawler?.filePath
                    ?: "")
            if (cloudHubConfigJS != null) {
                // 添加数据库
                if (HubDatabaseManager.addDatabase(cloudHubConfigGson, cloudHubConfigJS)) {
                    3
                } else -3
            } else -2
        } else -1).apply {
            GlobalScope.launch(Dispatchers.Main) {
                when (this@apply) {
                    3 -> Toast.makeText(context, "数据添加成功", Toast.LENGTH_LONG).show()
                    -1 -> Toast.makeText(context, "获取基础配置文件失败", Toast.LENGTH_LONG).show()
                    -2 -> Toast.makeText(context, "获取 JS 代码失败", Toast.LENGTH_LONG).show()
                    -3 -> Toast.makeText(context, "什么？数据库添加失败！", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * @return 1 获取 AppConfig 成功, 2 添加数据库成功, -1 获取 AppConfig 失败, -2 添加数据库失败
     */
    fun downloadCloudAppConfig(appUuid: String?): Int {
        val context = MyApplication.context
        GlobalScope.launch(Dispatchers.Main) { Toast.makeText(context, "开始下载", Toast.LENGTH_LONG).show() }  // 下载
        val cloudHubConfigGson = getAppCloudConfig(appUuid)
        return (if (cloudHubConfigGson != null) {
            // 添加数据库
            if (AppDatabaseManager.setDatabase(0, cloudHubConfigGson)) {
                2
            } else -2
        } else -1).apply {
            runBlocking(Dispatchers.Main) {
                when (this@apply) {
                    2 -> Toast.makeText(context, "数据添加成功", Toast.LENGTH_LONG).show()
                    -1 -> Toast.makeText(context, "获取基础配置文件失败", Toast.LENGTH_LONG).show()
                    -2 -> Toast.makeText(context, "什么？数据库添加失败！", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}