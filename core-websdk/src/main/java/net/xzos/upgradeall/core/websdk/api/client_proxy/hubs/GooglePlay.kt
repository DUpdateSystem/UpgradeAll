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
import net.xzos.upgradeall.core.utils.coroutines.ValueMutex
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
import net.xzos.upgradeall.core.utils.getLocale
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.websdk.api.client_proxy.APK_CONTENT_TYPE
import net.xzos.upgradeall.core.websdk.api.client_proxy.StringEncoder
import net.xzos.upgradeall.core.websdk.api.client_proxy.versionCode
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.http.OkHttpApi
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.HubData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

class GooglePlay(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "65c2f60c-7d08-48b8-b4ba-ac6ee924f6fa"

    override fun checkAppAvailable(hub: HubData, app: AppData): Boolean {
        val appPackage = app.appId[ANDROID_APP_TYPE] ?: return false
        val request = OkHttpApi.getRequestBuilder()
            .url("https://play.google.com/store/apps/details?id=$appPackage")
            .head().build()
        return OkHttpApi.call(request).execute().code != 404
    }

    override fun getReleases(hub: HubData, app: AppData): List<ReleaseGson>? {
        val appPackage = app.appId[ANDROID_APP_TYPE] ?: return emptyList()
        Log.i(logObjectTag, TAG, "getRelease: package: $appPackage")
        val authJson = getAuthJson(hub) ?: return null
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
        hub: HubData, app: AppData,
        assetIndex: List<Int>, assetGson: AssetGson?
    ): List<DownloadItem>? {
        val appPackage = app.appId[ANDROID_APP_TYPE] ?: return emptyList()
        val authJson = getAuthJson(hub) ?: return null
        val authData = getAuthData(authJson) ?: return null
        val detailHelper = AppDetailsHelper(authData)
        val app = detailHelper.getAppByPackageName(appPackage)
        return PurchaseHelper(authData).purchase(
            app.packageName, app.versionCode, app.offerType
        ).map {
            DownloadItem(it.name, it.url, null, null)
        }
    }

    private fun getAuthJson(hub: HubData): Map<String, String?>? {
        return hub.auth[EMAIL_AUTH]?.let {
            hub.auth[TOKEN_AUTH]?.run {
                mapOf(
                    EMAIL_AUTH to hub.auth[EMAIL_AUTH],
                    TOKEN_AUTH to hub.auth[TOKEN_AUTH]
                )
            }
        } ?: getAuthJsonFromWeb(hub.other[AUTH_SERVER] ?: "https://auroraoss.com/api/auth")
    }

    private fun getAuthJsonFromWeb(url: String): Map<String, String>? {
        return dataCache.get(mutex, SaveMode.DISK_ONLY, "AuroraOSS_auth", StringEncoder) {
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

    companion object {
        private const val TAG = "GooglePlay"

        private const val EMAIL_AUTH = "email"
        private const val TOKEN_AUTH = "auth"

        private const val AUTH_SERVER = "auth_server"
        private val logObjectTag = ObjectTag(core, TAG)

        private val mutex = ValueMutex()
    }
}