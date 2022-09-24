package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import com.aurora.gplayapi.DeviceManager
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.providers.DeviceInfoProvider
import com.aurora.gplayapi.exceptions.ApiException
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.constant.VERSION_CODE
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
import net.xzos.upgradeall.core.utils.data_cache.utils.StringEncoder
import net.xzos.upgradeall.core.utils.getLocale
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.websdk.api.client_proxy.APK_CONTENT_TYPE
import net.xzos.upgradeall.core.websdk.api.client_proxy.versionCode
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

class GooglePlay(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "65c2f60c-7d08-48b8-b4ba-ac6ee924f6fa"

    private fun getAuthJson(data: ApiRequestData): Map<String, String?>? {
        return data.auth[EMAIL_AUTH]?.let {
            data.auth[TOKEN_AUTH]?.run {
                mapOf(
                    EMAIL_AUTH to data.auth[EMAIL_AUTH],
                    TOKEN_AUTH to data.auth[TOKEN_AUTH]
                )
            }
        } ?: getAuthJsonFromWeb(data.other.get(AUTH_SERVER) ?: "https://auroraoss.com/api/auth")
    }

    private fun getAuthJsonFromWeb(url: String): Map<String, String>? {
        return dataCache.get("AuroraOSS_auth", SaveMode.DISK_ONLY, StringEncoder) {
            okhttpProxy.okhttpExecute(HttpRequestData(url))?.body?.string()
        }?.let {
            return@let Gson().fromJson(it, object : TypeToken<Map<String, String>?>() {}.type)
        }
    }

    private fun getAuthData(
        authJson: Map<String, String?>,
        deviceName: String = "px_3a"
    ): AuthData? {
        val email = authJson["email"] ?: return null
        val auth = authJson["auth"] ?: return null
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
        val authJson = getAuthJson(data) ?: return null
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
                changelog = app.changes,
                assetGsonList = app.fileList.map {
                    AssetGson(
                        fileName = it.name,
                        fileType = APK_CONTENT_TYPE,
                        downloadUrl = it.url,
                    )
                }
            ).versionCode(app.versionCode)
        )
    }

    override fun getDownload(
        data: ApiRequestData,
        assetIndex: List<Int>,
        assetGson: AssetGson?
    ): List<DownloadItem>? {
        val appPackage = data.appId[ANDROID_APP_TYPE] ?: return emptyList()
        val authJson = getAuthJson(data) ?: return null
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

        private const val EMAIL_AUTH = "email"
        private const val TOKEN_AUTH = "auth"

        private const val AUTH_SERVER = "auth_server"
        private val logObjectTag = ObjectTag(core, TAG)
    }
}