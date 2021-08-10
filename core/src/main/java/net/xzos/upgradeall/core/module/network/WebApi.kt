package net.xzos.upgradeall.core.module.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.data.json.DownloadItem
import net.xzos.upgradeall.core.data.json.ReleaseGson
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.log.ObjectTag.Companion.core
import java.net.SocketTimeoutException

internal object WebApi {
    private val host = coreConfig.update_server_url
    private const val TAG = "WebApi"
    private val objectTag = ObjectTag(core, TAG)

    fun getCloudConfig(): String? {
        val url = "http://$host/v1/rules/download/dev"
        return callApiCore {
            OkHttpApi.get(url, retryNum = 3)
        }?.body?.string()
    }

    fun getAppRelease(
        hubUuid: String, auth: Map<String, String>,
        appId: Map<String, String>, callback: (ReleaseGson?) -> Unit
    ) {
        val appIdPath = getMapPath(appId)
        val authHeader = getAuthHeaderDict(auth)
        val url = "http://$host/v1/app/$hubUuid/${appIdPath}/release"
        val response = callApiCore { OkHttpApi.get(url, authHeader, retryNum = 3) }
        val responseStr = response?.body?.string()
        val release = Gson().fromJson(responseStr, ReleaseGson::class.java)
        callback(release)
    }

    fun getAppReleaseList(
        hubUuid: String, auth: Map<String, String>,
        appId: Map<String, String>, callback: (List<ReleaseGson>?) -> Unit
    ) {
        val appIdPath = getMapPath(appId)
        val url = "http://$host/v1/app/$hubUuid/${appIdPath}/releases"
        val authHeader = getAuthHeaderDict(auth)
        val response = callApiCore { OkHttpApi.get(url, authHeader, retryNum = 3) }
        val responseStr = response?.body?.string()
        val listType = object : TypeToken<ArrayList<ReleaseGson>>() {}.type
        val releaseList = Gson().fromJson<List<ReleaseGson>>(responseStr, listType)
        callback(releaseList)
    }

    fun getDownloadInfo(
        hubUuid: String, auth: Map<String, String>,
        appId: Map<String, String>, assetIndex: Pair<Int, Int>
    ): List<DownloadItem> {
        val appIdPath = getMapPath(appId)
        val authHeader = getAuthHeaderDict(auth)
        val assetIndexPath = getIntListPath(assetIndex.toList())
        val url = "http://$host/v1/app/$hubUuid/${appIdPath}/extra_download/$assetIndexPath"
        val response = callApiCore { OkHttpApi.get(url, authHeader, retryNum = 3) }
        val responseStr = response?.body?.string()
        val listType = object : TypeToken<ArrayList<DownloadItem>>() {}.type
        return Gson().fromJson(responseStr, listType)
    }

    private fun <E> callApiCore(core: () -> E): E? {
        return try {
            core()
        } catch (e: SocketTimeoutException) {
            null
        }
    }

    private fun getMapPath(appId: Map<String, String?>): String {
        var appIdPath = ""
        for ((k, v) in appId) {
            appIdPath += "/$k/$v"
        }
        return appIdPath.replaceFirst("/", "")
    }

    private fun getAuthHeaderDict(auth: Map<String, String>): Map<String, String> {
        return mapOf("Authorization" to Gson().toJson(auth))
    }

    private fun getIntListPath(list: Collection<Int>) = list.joinToString("/")
}