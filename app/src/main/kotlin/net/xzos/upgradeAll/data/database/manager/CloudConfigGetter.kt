package net.xzos.upgradeAll.data.database.manager

import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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

    private var rulesListJsonFileRawUrl: String = getRawRootUrl(
            PreferenceManager.getDefaultSharedPreferences(MyApplication.context).getString(
                    "cloud_rules_hub_url",
                    MyApplication.context.resources.getString(R.string.default_cloud_rules_hub_url)
            )
    ) + "rules/rules_list.json"
    private var cloudConfig: CloudConfig? = null
        get() {
            if (field == null)
                field = renewCloudConfig()
            return field
        }

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

    fun getAppConfig(appConfigName: String): AppConfig? {
        val appConfigString = okHttpApi.getHttpResponse(
                "${cloudConfig?.listUrl?.appListRawUrl}$appConfigName.json"
        ).first
        return try {
            Gson().fromJson(appConfigString, AppConfig::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    fun getHubConfig(hubConfigName: String): HubConfig? {
        val hubConfigString = okHttpApi.getHttpResponse(
                "${cloudConfig?.listUrl?.hubListRawUrl}$hubConfigName.json"
        ).first
        return try {
            Gson().fromJson(hubConfigString, HubConfig::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }

    }

    fun getHubConfigJS(filePath: String): String? {
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
}