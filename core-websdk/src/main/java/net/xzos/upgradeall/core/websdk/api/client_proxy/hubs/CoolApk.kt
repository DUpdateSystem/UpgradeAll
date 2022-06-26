package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import android.util.Base64
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.getOrNull
import net.xzos.upgradeall.core.utils.iterator
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.md5
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.Assets
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant

internal class CoolApk(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "1c010cc9-cff8-4461-8993-a86cd190d377"

    override fun getRelease(
        data: ApiRequestData,
    ): List<ReleaseGson>? {
        val appPackage = data.appId[ANDROID_APP_TYPE] ?: return emptyList()

        // get latest
        val detailUrl = "https://api.coolapk.com/v6/apk/detail?id=$appPackage"
        val response = httpGet(detailUrl)
        val json = response?.body?.string()?.let { JSONObject(it) } ?: return null
        if (json.getOrNull("status", json::getInt) == -2) return emptyList()
        val detail = json.getOrNull("data", json::getJSONObject) ?: return null
        val aid = detail.getString("id")
        val latestVersionNumber = detail.getString("apkversionname")
        val releaseList = mutableListOf<ReleaseGson>()
        releaseList.add(
            ReleaseGson(
                versionNumber = latestVersionNumber,
                changelog = detail.getString("changelog"),
                assetList = getLatestAsset(appPackage, aid, latestVersionNumber)?.let {
                    listOf(it)
                } ?: emptyList()
            )
        )

        // get history
        val historyUrl = "https://api.coolapk.com/v6/apk/downloadVersionList?id=${aid}"
        val historyResponse = httpGet(historyUrl)
        val historyJsonObject = historyResponse?.body?.string()?.let { JSONObject(it) }
            ?: return releaseList
        val historyJsonList = historyJsonObject.getOrNull("data", historyJsonObject::getJSONArray)
            ?: return releaseList
        for (historyJson in historyJsonList.iterator<JSONObject>()) {
            val versionName = historyJson.getString("versionName")
            val versionId = historyJson.getString("versionId")
            releaseList.add(
                ReleaseGson(
                    versionNumber = versionName,
                    changelog = null,
                    assetList = getHistoryDownloadUrl(
                        appPackage, aid, versionId, versionName
                    )?.let {
                        listOf(it)
                    } ?: emptyList()
                )
            )
        }
        return releaseList
    }

    private fun getLatestAsset(appPackage: String, aid: String, versionName: String): Assets? {
        val url = "https://api.coolapk.com/v6/apk/download?pn=$appPackage&aid=$aid"
        val request = httpRedirects(url) ?: return null
        return getAsset(request, appPackage, versionName)
    }

    private fun getHistoryDownloadUrl(
        appPackage: String, aid: String, versionId: String, versionName: String
    ): Assets? {
        val url =
            "https://api.coolapk.com/v6/apk/downloadHistory?pn=$appPackage&aid=$aid&versionId=$versionId&downloadFrom=coolapk"
        val request = httpRedirects(url) ?: return null
        return getAsset(request, appPackage, versionName)
    }

    override fun getDownload(
        data: ApiRequestData,
        assetIndex: List<Int>,
        assets: Assets?
    ): List<DownloadItem>? {
        val request = assets?.downloadUrl?.let { httpRedirects(it) }
        if (request?.headers?.get("Content-Type")?.split(";")
                ?.get(0) != "application/vnd.android.package-archive"
        ) {
            Log.i(logObjectTag, TAG, "getDownload: 返回非安装包数据")
            val newAssets =
                getRelease(data)?.get(assetIndex[0])?.assetList?.get(assetIndex[1])
                    ?: return null
            return listOf(
                DownloadItem(
                    newAssets.fileName, newAssets.downloadUrl ?: return null, null, null
                )
            )
        } else {
            Log.i(logObjectTag, TAG, "getDownload: 网址验证正确")
            return listOf(DownloadItem(assets.fileName, request.url.toString(), null, null))
        }
    }

    private fun getAsset(request: Request, appPackage: String, versionName: String) = Assets(
        fileName = "${appPackage}_$versionName.apk",
        fileType = request.headers["Content-Type"]?.split(";")?.get(0),
        downloadUrl = request.url.toString()
    )

    private fun httpRedirects(url: String) =
        okhttpProxy.okhttpExecute(HttpRequestData(url, headerMap))?.request

    private fun httpGet(url: String) = okhttpProxy.okhttpExecute(HttpRequestData(url, headerMap))

    companion object {
        private const val TAG = "CoolApk"
        private val logObjectTag = ObjectTag(core, TAG)

        // 加密算法来自 https://github.com/ZCKun/CoolapkTokenCrack、https://zhuanlan.zhihu.com/p/69195418

        private val headerMap
            get() = mapOf(
                "User-Agent" to "Dalvik/2.1.0 (Linux; U; Android 9; MI 8 SE MIUI/9.5.9) (#Build; Xiaomi; MI 8 SE; PKQ1.181121.001; 9) +CoolMarket/9.2.2-1905301",
                "X-App-Id" to "com.coolapk.market",
                "X-Requested-With" to "XMLHttpRequest",
                "X-Sdk-Int" to "28",
                "X-Sdk-Locale" to "zh-CN",
                "X-Api-Version" to "9",
                "X-App-Version" to "9.2.2",
                "X-App-Code" to "1903501",
                "X-App-Device" to "QRTBCOgkUTgsTat9WYphFI7kWbvFWaYByO1YjOCdjOxAjOxEkOFJjODlDI7ATNxMjM5MTOxcjMwAjN0AyOxEjNwgDNxITM2kDMzcTOgsTZzkTZlJ2MwUDNhJ2MyYzM",
                "Host" to "api.coolapk.com",
                "X-Dark-Mode" to "0",
                "X-App-Token" to getAppToken(),
            )

        private const val DEVICE_ID = "55077056-48ee-46c8-80a6-2a21a9c5b12b"

        private fun getAppToken(): String {
            val t = Instant.now().epochSecond
            val tHex = "0x${t.toString(16)}"
            // 时间戳加密
            val tMd5 = t.toString().md5()
            // 不知道什么鬼字符串拼接
            val a =
                "token://com.coolapk.market/c67ef5943784d09750dcfbb31020f0ab?$tMd5$$DEVICE_ID&com.coolapk.market"

            // 不知道什么鬼字符串拼接 后的字符串再次加密
            val aMd5 = Base64.encodeToString(a.toByteArray(), Base64.NO_WRAP).md5()
            return "$aMd5$DEVICE_ID$tHex"
        }
    }
}