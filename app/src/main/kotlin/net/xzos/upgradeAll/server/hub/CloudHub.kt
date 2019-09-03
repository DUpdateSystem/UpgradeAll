package net.xzos.upgradeAll.server.hub

import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.json.gson.CloudConfig
import net.xzos.upgradeAll.json.gson.HubConfig
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.js.utils.OkHttpApi
import net.xzos.upgradeAll.utils.FileUtil

class CloudHub {
    private var rulesListJsonFileRawUrl: String
    private var cloudConfig: CloudConfig? = null

    val appList: List<CloudConfig.AppListBean>?
        get() = cloudConfig?.appList

    val hubList: List<CloudConfig.HubListBean>?
        get() = cloudConfig?.hubList

    init {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(MyApplication.context)
        val defCloudRulesHubUrl = MyApplication.context.resources.getString(R.string.default_cloud_rules_hub_url)
        val gitUrl = sharedPref.getString("cloud_rules_hub_url", defCloudRulesHubUrl)
        val baseRawUrl = getRawRootUrl(gitUrl)
        rulesListJsonFileRawUrl = baseRawUrl + "rules/rules_list.json"
    }

    fun getCloudConfig(): Boolean {
        var isSuccess = false
        val jsonText = OkHttpApi.getHttpResponse(LogObjectTag, rulesListJsonFileRawUrl).first
        // 如果刷新失败，则不记录数据
        if (jsonText != null && jsonText.isNotEmpty()) {
            try {
                cloudConfig = Gson().fromJson(jsonText, CloudConfig::class.java)
                isSuccess = true
            } catch (e: JsonSyntaxException) {
                Log.e(LogObjectTag, TAG, "refreshData: ERROR_MESSAGE: $e")
            }
        }
        return isSuccess
    }

    fun getAppConfig(packageName: String): String? {
        val appConfigRawUrl = cloudConfig?.listUrl?.appListRawUrl + packageName + ".json"
        return OkHttpApi.getHttpResponse(LogObjectTag, appConfigRawUrl).first
    }

    fun getHubConfig(hubConfigName: String): HubConfig? {
        val hubConfigRawUrl = """${cloudConfig?.listUrl?.hubListRawUrl}$hubConfigName.json"""
        val hubConfigString = OkHttpApi.getHttpResponse(LogObjectTag, hubConfigRawUrl).first
        return try {
            Gson().fromJson(hubConfigString, HubConfig::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }

    }

    fun getHubConfigJS(filePath: String): String? {
        val hubListRawUrl = cloudConfig?.listUrl?.hubListRawUrl ?: return null
        val hubConfigJSRawUrl = FileUtil.pathTransformRelativeToAbsolute(hubListRawUrl, filePath)
        return OkHttpApi.getHttpResponse(LogObjectTag, hubConfigJSRawUrl).first
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

    companion object {

        private const val TAG = "CloudHub"
        private val LogObjectTag = arrayOf("Core", TAG)

        private val Log = ServerContainer.Log
    }
}
