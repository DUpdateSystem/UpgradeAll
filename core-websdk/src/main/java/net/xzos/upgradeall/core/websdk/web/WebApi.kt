package net.xzos.upgradeall.core.websdk.web

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import net.xzos.upgradeall.core.websdk.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.web.proxy.OkhttpProxy

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
            return Gson().fromJson(response.body?.string(), CloudConfigList::class.java)
        } catch (e: Throwable) {
            Log.e(objectTag, TAG, "getCloudConfig: Error: ${e.msg()}")
            null
        }
    }

    fun getAppRelease(data: HttpRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        okhttpAsync(data, {
            when (it?.code) {
                200 -> {
                    try {
                        val responseStr = it.bodyStr
                        val release = Gson().fromJson(responseStr, ReleaseGson::class.java)
                        callback(listOf(release))
                    } catch (e: Throwable) {
                        Log.e(objectTag, TAG, "getAppRelease: Error: ${e.msg()}")
                        callback(null)
                    }
                }
                410 -> callback(emptyList())
                else -> {
                    Log.e(objectTag, TAG, "getAppRelease: $it")
                    callback(null)
                }
            }
        })
    }

    fun getAppReleaseList(data: HttpRequestData, callback: (List<ReleaseGson>?) -> Unit) {
        okhttpAsync(data, {
            when (it?.code) {
                200 -> {
                    try {
                        val responseStr = it.bodyStr
                        val releaseList =
                            Gson().fromJson<List<ReleaseGson>>(responseStr, releaseListType)
                        callback(releaseList)
                    } catch (e: Throwable) {
                        Log.e(objectTag, TAG, "getAppReleaseList: Error: ${e.msg()}")
                        callback(null)
                    }
                }
                410 -> callback(emptyList())
                else -> {
                    Log.e(objectTag, TAG, "getAppReleaseList: $it")
                    callback(null)
                }
            }
        })
    }

    fun getDownloadInfo(
        data: HttpRequestData,
    ): List<DownloadItem> {
        val response = okhttpExecuteNoError(data)
        if (response?.code != 200) return emptyList()
        try {
            val responseStr = response.body?.string()
            if (responseStr.isNullOrBlank()) return emptyList()
            return Gson().fromJson(responseStr, downloadListType)
        } catch (e: Throwable) {
            Log.e(objectTag, TAG, "getDownloadInfo: Error: ${e.msg()}")
            throw e
        }
    }
}