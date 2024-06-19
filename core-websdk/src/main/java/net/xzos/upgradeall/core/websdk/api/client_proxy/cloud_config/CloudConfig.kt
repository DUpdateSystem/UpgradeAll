package net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config

import net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.migration.app1to2
import net.xzos.upgradeall.core.websdk.api.client_proxy.cloud_config.migration.hub5to6
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import org.json.JSONObject

internal class CloudConfig(private val okHttpApi: OkhttpProxy) {
    fun getCloudConfig(url: String): CloudConfigList? {
        val response = okHttpApi.okhttpExecute(HttpRequestData(url))
        val str = response?.body?.string() ?: return null
        val json = JSONObject(str)
        val appJsonList = json.getJSONArray("app_config_list")
        val hubJsonList = json.getJSONArray("hub_config_list")
        val appList = mutableListOf<net.xzos.upgradeall.websdk.data.json.AppConfigGson>()
        val hubList = mutableListOf<net.xzos.upgradeall.websdk.data.json.HubConfigGson>()
        for (i in 0 until appJsonList.length()) {
            val appJson = appJsonList.getJSONObject(i)
            app1to2(appJson)?.let { appList.add(it) }
        }
        for (i in 0 until hubJsonList.length()) {
            val hubJson = hubJsonList.getJSONObject(i)
            hub5to6(hubJson)?.let { hubList.add(it) }
        }
        return CloudConfigList(appList, hubList)
    }
}