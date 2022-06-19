package net.xzos.upgradeall.core.websdk.api.web

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

internal class WebApi : OkhttpProxy() {

    companion object {
        private const val TAG = "WebApi"
        private val objectTag = ObjectTag(core, TAG)

        private val releaseListType = object : TypeToken<ArrayList<ReleaseGson>>() {}.type
        private val downloadListType = object : TypeToken<ArrayList<DownloadItem>>() {}.type
    }

    fun getCloudConfig(url: String): CloudConfigList? {
        val response = okhttpExecute(HttpRequestData(url))
        if (response?.code != 200) return null
        return try {
            return Gson().fromJson(response.body.string(), CloudConfigList::class.java)
        } catch (e: Throwable) {
            Log.e(objectTag, TAG, "getCloudConfig: Error: ${e.msg()}")
            null
        }
    }

    fun getAppRelease(data: HttpRequestData): List<ReleaseGson>? {
        val response = okhttpExecute(data)
        return when (response?.code) {
            200 -> {
                try {
                    val responseStr = response.body.string()
                    val release = Gson().fromJson(responseStr, ReleaseGson::class.java)
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

    fun getAppReleaseList(data: HttpRequestData): List<ReleaseGson>? {
        val response = okhttpExecute(data)
        return when (response?.code) {
            200 -> {
                try {
                    val responseStr = response.body.string()
                    Gson().fromJson<List<ReleaseGson>>(responseStr, releaseListType)
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
    ): List<DownloadItem> {
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