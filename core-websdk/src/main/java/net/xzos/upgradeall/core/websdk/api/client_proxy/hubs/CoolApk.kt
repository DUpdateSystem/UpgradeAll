package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.utils.base64
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.constant.VERSION_CODE
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.getOrNull
import net.xzos.upgradeall.core.utils.iterator
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.md5
import net.xzos.upgradeall.core.websdk.api.client_proxy.APK_CONTENT_TYPE
import net.xzos.upgradeall.core.websdk.api.client_proxy.versionCode
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.http.OkHttpApi
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.HubData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import okhttp3.Request
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.util.*


internal class CoolApk(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "1c010cc9-cff8-4461-8993-a86cd190d377"

    override fun checkAppAvailable(hub: HubData, app: AppData): Boolean {
        val appPackage = app.appId[ANDROID_APP_TYPE] ?: return false
        val request = OkHttpApi.getRequestBuilder().url("https://www.coolapk.com/apk/$appPackage")
            .head().build()
        return OkHttpApi.call(request).execute().code != 404
    }

    override fun getUpdate(hub: HubData, appList: List<AppData>): Map<AppData, ReleaseGson> {
        TODO("Not yet implemented")
    }

    override fun getReleases(hub: HubData, app: AppData): List<ReleaseGson>? {
        val appPackage = app.appId[ANDROID_APP_TYPE] ?: return emptyList()

        // get latest
        val detailUrl = "$apiUrl/v6/apk/detail?id=$appPackage"
        @Suppress("SameParameterValue") val response = httpGet(detailUrl)
        val json = response?.body?.string()?.let { JSONObject(it) } ?: return null
        if (json.getOrNull("status", json::getInt) == -2) return emptyList()
        val detail = json.getOrNull("data", json::getJSONObject) ?: return null
        val aid = detail.getString("id")
        val latestVersionNumber = detail.getString("apkversionname")
        val releaseList = mutableListOf<ReleaseGson>()
        releaseList.add(
            ReleaseGson(
                versionNumber = latestVersionNumber,
                extra = mapOf(VERSION_CODE to detail.getLong("apkversioncode")),
                changelog = detail.getString("changelog"),
                assetGsonList = getLatestAsset(appPackage, aid, latestVersionNumber)?.let {
                    listOf(it)
                } ?: emptyList()
            )
        )

        // get history
        val historyUrl = "$apiUrl/v6/apk/downloadVersionList?id=${aid}"
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
                    assetGsonList = getHistoryDownloadUrl(
                        appPackage, aid, versionId, versionName
                    )?.let {
                        listOf(it)
                    } ?: emptyList()
                ).versionCode(historyJson.getLong("versionCode"))
            )
        }
        return releaseList
    }

    private fun getLatestAsset(appPackage: String, aid: String, versionName: String): AssetGson? {
        val url = "$apiUrl/v6/apk/download?pn=$appPackage&aid=$aid"
        val request = httpRedirects(url) ?: return null
        return getAsset(request, appPackage, versionName)
    }

    private fun getHistoryDownloadUrl(
        appPackage: String, aid: String, versionId: String, versionName: String
    ): AssetGson? {
        @Suppress("SameParameterValue") val url =
            "$apiUrl/v6/apk/downloadHistory?pn=$appPackage&aid=$aid&versionId=$versionId&downloadFrom=coolapk"
        val request = httpRedirects(url) ?: return null
        return getAsset(request, appPackage, versionName)
    }

    override fun getDownload(
        hub: HubData, app: AppData,
        assetIndex: List<Int>, assetGson: AssetGson?
    ): List<DownloadItem>? {
        val request = assetGson?.downloadUrl?.let { httpRedirects(it) }
        if (request?.headers?.get("Content-Type")?.split(";")?.get(0) != APK_CONTENT_TYPE) {
            Log.i(logObjectTag, TAG, "getDownload: 返回非安装包数据")
            val newAssets =
                getReleases(hub, app)?.get(assetIndex[0])?.assetGsonList?.get(assetIndex[1])
                    ?: return null
            return listOf(
                DownloadItem(
                    newAssets.fileName, newAssets.downloadUrl ?: return null, null, null
                )
            )
        } else {
            Log.i(logObjectTag, TAG, "getDownload: 网址验证正确")
            return listOf(DownloadItem(assetGson.fileName, request.url.toString(), null, null))
        }
    }

    private fun getAsset(request: Request, appPackage: String, versionName: String) = AssetGson(
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

        // 加密算法来自 https://github.com/XiaoMengXinX/FuckCoolapkTokenV2、https://github.com/Coolapk-UWP/Coolapk-UWP

        private const val manufactor = "Google"
        private const val brand = "Google"
        private const val model = "Pixel 5a"
        private const val buildNumber = "SQ1D.220105.007"

        private fun getToken(): Pair<String, String> {
            val randDeviceCode = createDeviceCode(
                randHexString(16),
                randMacAdress(),
                manufactor,
                brand,
                model,
                buildNumber
            )
            return Pair(
                randDeviceCode,
                getTokenWithDeviceCode(randDeviceCode)
            )
        }

        private fun getTokenWithDeviceCode(deviceCode: String): String {
            val timeStamp = "${System.currentTimeMillis() / 1000}"
            val base64TimeStamp = timeStamp.base64()
            val md5TimeStamp = timeStamp.md5()
            val md5DeviceCode = deviceCode.md5()

            val token =
                "token://com.coolapk.market/dcf01e569c1e3db93a3d0fcf191a622c?$md5TimeStamp$$md5DeviceCode&com.coolapk.market"
            val base64Token = token.base64()
            val md5Base64Token = base64Token.md5()
            val md5Token = token.md5()

            val bcryptSalt =
                "$2a$10$${base64TimeStamp.substring(0, 14)}/${md5Token.substring(0, 6)}u"

            val bcryptresult = BCrypt.hashpw(md5Base64Token, bcryptSalt)
            val reBcryptresult = bcryptresult.replaceRange(0, 3, "$2y")
            return "v2${reBcryptresult.base64()}"
        }

        @Suppress("SameParameterValue")
        private fun createDeviceCode(
            aid: String,
            mac: String,
            manufactor: String,
            brand: String,
            model: String,
            buildNumber: String
        ) = "$aid; ; ; $mac; $manufactor; $brand; $model; $buildNumber".base64()

        private val rand = Random()

        private fun randHexString(@Suppress("SameParameterValue") n: Int): String {
            rand.setSeed(System.currentTimeMillis())
            return (0 until n).joinToString("") {
                rand.nextInt(256).toString(16)
            }.uppercase()
        }

        private fun randMacAdress() = (0 until 6).toList().joinToString(":") {
            String.format("%02x", rand.nextInt(256))
        }
    }

    // CoolAPK header
    private val headerMap
        get() = mapOf(
            "User-Agent" to "Dalvik/2.1.0 (Linux; U; Android 9; MI 8 SE MIUI/9.5.9) (#Build; Xiaomi; MI 8 SE; PKQ1.181121.001; 9) +CoolMarket/12.4.2-2208241-universal",
            "X-App-Id" to "com.coolapk.market",
            "X-Requested-With" to "XMLHttpRequest",
            "X-Sdk-Int" to "30",
            "X-App-Mode" to "universal",
            "X-App-Channel" to "coolapk",
            "X-Sdk-Locale" to "zh-CN",
            "X-App-Version" to "12.4.2",
            "X-Api-Supported" to "2208241",
            "X-App-Code" to "2208241",
            "X-Api-Version" to "12",
            "X-App-Device" to deviceCode,
            "X-Dark-Mode" to "0",
            "X-App-Token" to reqToken,
        )


    private val tokenPair = getToken()
    private val deviceCode get() = tokenPair.first
    private val reqToken get() = tokenPair.second

    private val apiUrl = "https://api2.coolapk.com"
}