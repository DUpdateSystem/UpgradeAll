package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import com.aurora.gplayapi.DeviceManager
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.providers.DeviceInfoProvider
import com.aurora.gplayapi.exceptions.ApiException
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.constant.VERSION_CODE
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.utils.JsonObjectEncoder
import net.xzos.upgradeall.core.utils.getLocale
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.websdk.api.client_proxy.APK_CONTENT_TYPE
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import org.json.JSONObject

class GooglePlay(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "65c2f60c-7d08-48b8-b4ba-ac6ee924f6fa"

    private fun getAuthJson(url: String = "https://auroraoss.com/api/auth"): JSONObject? {
        return dataCache.get("AuroraOSS_auth", JsonObjectEncoder) {
            okhttpProxy.okhttpExecute(HttpRequestData(url))?.body?.string()
                ?.let { JSONObject(it) }
        }
    }

    private fun getAuthData(authJson: JSONObject, deviceName: String = "px_3a"): AuthData? {
        val email = authJson.getString("email")
        val auth = authJson.getString("auth")
        Log.i(logObjectTag, TAG, "getAuthData: email: $email")
        val properties = DeviceManager.loadProperties("$deviceName.properties") ?: return null
        val locale = getLocale()
        Log.i(logObjectTag, TAG, "getAuthData: locale: $locale")
        val deviceInfoProvider = DeviceInfoProvider(properties, locale.toString())
        return AuthHelper.buildInsecure(email, auth, locale, deviceInfoProvider)
    }

    override fun getRelease(data: ApiRequestData): List<ReleaseGson>? {
        val appPackage = data.appId[ANDROID_APP_TYPE] ?: return emptyList()
        Log.i(logObjectTag, TAG, "getRelease: package: $appPackage")
        val authJson = getAuthJson() ?: return null
        val authData = getAuthData(authJson) ?: return null
        val detailHelper = AppDetailsHelper(authData)
        val app = try {
            detailHelper.getAppByPackageName(appPackage)
        } catch (e: ApiException.AppNotFound) {
            return emptyList()
        }
        Log.i(logObjectTag, TAG, "getRelease: appInfo: ${app.appInfo.appInfoMap}")
        return listOf(
            ReleaseGson(
                versionNumber = app.versionName,
                extra = mapOf(VERSION_CODE to app.versionCode),
                changelog = app.changes,
                assetGsonList = app.fileList.map {
                    AssetGson(
                        fileName = it.name,
                        fileType = APK_CONTENT_TYPE,
                        downloadUrl = it.url,
                    )
                }
            )
        )
    }

    override fun getDownload(
        data: ApiRequestData,
        assetIndex: List<Int>,
        assetGson: AssetGson?
    ): List<DownloadItem>? {
        val appPackage = data.appId[ANDROID_APP_TYPE] ?: return emptyList()
        val authJson = getAuthJson() ?: return null
        val authData = getAuthData(authJson) ?: return null
        val detailHelper = AppDetailsHelper(authData)
        val app = detailHelper.getAppByPackageName(appPackage)
        return PurchaseHelper(authData).purchase(
            app.packageName,
            app.versionCode,
            app.offerType
        ).map {
            DownloadItem(it.name, it.url, null, null)
        }
    }

    companion object {
        private const val TAG = "GooglePlay"
        private val logObjectTag = ObjectTag(core, TAG)
    }
}