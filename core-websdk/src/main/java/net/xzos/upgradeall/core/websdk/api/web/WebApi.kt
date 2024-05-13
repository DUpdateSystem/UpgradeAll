package net.xzos.upgradeall.core.websdk.api.web

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy

internal class WebApi : OkhttpProxy() {

    companion object {
        private const val TAG = "WebApi"
        private val objectTag = ObjectTag(core, TAG)

        val releaseListType = object : TypeToken<ArrayList<net.xzos.upgradeall.websdk.data.json.ReleaseGson>>() {}.type
        val downloadListType = object : TypeToken<ArrayList<net.xzos.upgradeall.websdk.data.json.DownloadItem>>() {}.type
    }

    fun getCloudConfig(url: String): net.xzos.upgradeall.websdk.data.json.CloudConfigList? {
        val response = okhttpExecute(HttpRequestData(url))
        if (response?.code != 200) return null
        return try {
            return Gson().fromJson(response.body.string(), net.xzos.upgradeall.websdk.data.json.CloudConfigList::class.java)
        } catch (e: Throwable) {
            Log.e(objectTag, TAG, "getCloudConfig: Error: ${e.msg()}")
            null
        }
    }

    fun getAppRelease(data: HttpRequestData): List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>? {
        val response = okhttpExecute(data)
        return when (response?.code) {
            200 -> {
                try {
                    val responseStr = response.body.string()
                    val release = Gson().fromJson(responseStr, net.xzos.upgradeall.websdk.data.json.ReleaseGson::class.java)
                    listOf(release)
                } catch (e: Throwable) {
                    Log.e(objectTag, TAG, "getAppRelease: Error: ${e.msg()}")
                    null
                }
            }
            410 -> emptyList()
            else -> {
                Log.e(objectTag, TAG, "getAppRelease: $response")
                null
            }
        }
    }

    fun getAppReleaseList(data: HttpRequestData): List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>? {
        val response = okhttpExecute(data)
        return when (response?.code) {
            200 -> {
                try {
                    val responseStr = response.body.string()
                    Gson().fromJson<List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>>(responseStr, releaseListType)
                } catch (e: Throwable) {
                    Log.e(objectTag, TAG, "getAppReleaseList: Error: ${e.msg()}")
                    null
                }
            }
            410 -> emptyList()
            else -> {
                Log.e(objectTag, TAG, "getAppReleaseList: $response")
                null
            }
        }
    }

    fun getDownloadInfo(
        data: HttpRequestData,
    ): List<net.xzos.upgradeall.websdk.data.json.DownloadItem> {
        val response = okhttpExecuteNoError(data)
        if (response?.code != 200) return emptyList()
        try {
            val responseStr = response.body.string()
            if (responseStr.isBlank()) return emptyList()
            return Gson().fromJson(responseStr, downloadListType)
        } catch (e: Throwable) {
            Log.e(objectTag, TAG, "getDownloadInfo: Error: ${e.msg()}")
            throw e
        }
    }
}