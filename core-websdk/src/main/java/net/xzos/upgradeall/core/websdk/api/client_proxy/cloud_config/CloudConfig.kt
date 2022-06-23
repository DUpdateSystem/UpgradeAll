package net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config

import com.google.gson.Gson
import net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.migration.app1to2
import net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.migration.hub5to6
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.json.AppConfigGson
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.HubConfigGson
import org.json.JSONObject

internal class CloudConfig(private val okHttpApi: OkhttpProxy) {
    fun getCloudConfig(url: String): CloudConfigList? {
        val response = okHttpApi.okhttpExecute(HttpRequestData(url))
        val str = response?.body?.string() ?: return null
        val json = JSONObject(str)
        val appJsonList = json.getJSONArray("app_config_list")
        val hubJsonList = json.getJSONArray("hub_config_list")
        val appList = mutableListOf<AppConfigGson>()
        val hubList = mutableListOf<HubConfigGson>()
        for (i in 0 until appJsonList.length()) {
            val appJson = appJsonList.getJSONObject(i)
            app1to2(appJson)?.toString()?.let {
                appList.add(gson.fromJson(it, AppConfigGson::class.java))
            }
        }
        for (i in 0 until hubJsonList.length()) {
            val hubJson = hubJsonList.getJSONObject(i)
            hub5to6(hubJson)?.toString()?.let {
                hubList.add(gson.fromJson(it, HubConfigGson::class.java))
            }
        }
        return CloudConfigList(appList, hubList)
    }

    companion object {
        private val gson = Gson()
    }
}