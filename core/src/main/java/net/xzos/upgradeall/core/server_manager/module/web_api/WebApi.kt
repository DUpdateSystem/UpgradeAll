package net.xzos.upgradeall.core.server_manager.module.web_api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.json.gson.WebApiGetGson
import net.xzos.upgradeall.core.data.json.gson.WebApiReturnGson
import net.xzos.upgradeall.core.data_manager.utils.DataCache
import okhttp3.OkHttpClient
import okhttp3.Request


class WebApi(private val hubUuid: String) {
    private val webApiUrl
        get() = "${AppConfig.update_server_url}/v1/$hubUuid"
    private var invalid = false

    suspend fun getReleaseInfo(appInfo: List<WebApiGetGson.AppInfoListBean>): List<WebApiReturnGson.ReleaseInfoBean>? {
        if (invalid) return null
        return getWebApiReturnGsonList(listOf(appInfo))?.get(0)?.releaseInfo
    }

    fun getWebApiReturnGsonList(
        appInfoList: List<List<WebApiGetGson.AppInfoListBean>>
    ): List<WebApiReturnGson>? {
        if (appInfoList.isEmpty()) return null
        val webApiReturnGsonList: MutableList<WebApiReturnGson> = mutableListOf()
        val noCacheAppInfoList: MutableList<List<WebApiGetGson.AppInfoListBean>> = mutableListOf()
        // 检查缓存
        for (appInfo in appInfoList) {
            if (DataCache.existsCache(hubUuid, appInfo)) {
                webApiReturnGsonList.add(
                    WebApiReturnGson(
                        appInfo = appInfo,
                        releaseInfo = DataCache.getReleaseInfo(hubUuid, appInfo)
                    )
                )
            } else noCacheAppInfoList.add(appInfo)
        }
        if (noCacheAppInfoList.isEmpty()) return webApiReturnGsonList
        // 获取在线数据
        for (appInfoChunkedList in noCacheAppInfoList.chunked(25)) {
            val appInfoHeader = Gson().toJson(
                appInfoChunkedList
            )
            val request = Request.Builder()
                .url(webApiUrl)
                .header(
                    "App-Info-List",
                    appInfoHeader
                )
                .build()
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.code == 404 && body == "NO_HUB") {
                    invalid = true
                    return null
                }
                webApiReturnGsonList.addAll(
                    Gson().fromJson(body, object : TypeToken<List<WebApiReturnGson>>() {}.type)
                )
            } catch (e: Throwable) {
                return null
            }
        }
        cacheData(webApiReturnGsonList)
        return webApiReturnGsonList
    }

    private fun cacheData(webApiReturnGsonList: List<WebApiReturnGson>) {
        for (webApiReturnGson in webApiReturnGsonList) {
            val appInfo = webApiReturnGson.appInfo
            DataCache.cacheReleaseInfo(hubUuid, appInfo, webApiReturnGson.releaseInfo)
        }
    }

    companion object {
        private val client = OkHttpClient()
    }
}
