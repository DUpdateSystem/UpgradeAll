package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import com.aurora.gplayapi.DeviceManager
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.providers.DeviceInfoProvider
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
import net.xzos.upgradeall.websdk.data.json.DownloadItem

class GooglePlay(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "65c2f60c-7d08-48b8-b4ba-ac6ee924f6fa"

    override fun checkAppAvailable(hub: HubData, app: AppData): Boolean {
        val appPackage = app.appId[ANDROID_APP_TYPE] ?: return false
        val request = OkHttpApi.getRequestBuilder()
            .url("https://play.google.com/store/apps/details?id=$appPackage").head().build()
        return OkHttpApi.call(request).execute().code != 404
    }

    data class ExceptionInfo(
        val packageName: String, val className: String, val reason: String, val code: Int? = null
    )

    private fun getDetailedExceptionInfo(e: Exception): ExceptionInfo {
        val exceptionClass = e.javaClass
        val packageName = exceptionClass.`package`?.name ?: ""
        val simpleName = exceptionClass.simpleName
        val className = when {
            simpleName.contains("AppNotFound") -> "AppNotFound"
            simpleName.contains("AuthException") -> "AuthException"
            simpleName.contains("AppNotPurchased") -> "AppNotPurchased"
            simpleName.contains("AppRemoved") -> "AppRemoved"
            simpleName.contains("AppNotSupported") -> "AppNotSupported"
            simpleName.contains("EmptyDownloads") -> "EmptyDownloads"
            simpleName.contains("Unknown") -> "Unknown"
            simpleName.contains("Server") -> "Server"
            else -> "UnknownException"
        }

        val reasonField = exceptionClass.declaredFields.find { it.name == "reason" }
        val codeField = exceptionClass.declaredFields.find { it.name == "code" }

        reasonField?.isAccessible = true
        codeField?.isAccessible = true

        val reason = reasonField?.get(e) as? String ?: e.message ?: "Unknown reason"
        val code = codeField?.get(e) as? Int

        return ExceptionInfo(packageName, className, reason, code)
    }

    override fun getReleases(
        hub: HubData, app: AppData
    ): List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>? {
        val appPackage = app.appId[ANDROID_APP_TYPE] ?: return emptyList()
        Log.i(logObjectTag, TAG, "getRelease: package: $appPackage")
        val authJson = getAuthJson(hub) ?: return null
        val authData = getAuthData(authJson) ?: return null
        val detailHelper = AppDetailsHelper(authData)
        val appDetail = try {
            detailHelper.getAppByPackageName(appPackage)
        } catch (e: Exception) {
            val exceptionInfo = getDetailedExceptionInfo(e)
            when (exceptionInfo.className) {
                "AppNotFound" -> println("App not found: ${exceptionInfo.reason}")
            }
            throw e
        }
        Log.i(logObjectTag, TAG, "getRelease: appInfo: ${appDetail.appInfo.appInfoMap}")
        return listOf(net.xzos.upgradeall.websdk.data.json.ReleaseGson(versionNumber = appDetail.versionName,
            changelog = appDetail.changes,
            assetGsonList = appDetail.fileList.map {
                net.xzos.upgradeall.websdk.data.json.AssetGson(
                    fileName = it.name,
                    fileType = APK_CONTENT_TYPE,
                    downloadUrl = it.url,
                )
            }).versionCode(appDetail.versionCode)
        )
    }

    override fun getDownload(
        hub: HubData,
        app: AppData,
        assetIndex: List<Int>,
        assetGson: net.xzos.upgradeall.websdk.data.json.AssetGson?
    ): List<DownloadItem>? {
        val appPackage = app.appId[ANDROID_APP_TYPE] ?: return emptyList()
        val authJson = getAuthJson(hub) ?: return null
        val authData = getAuthData(authJson) ?: return null
        val detailHelper = AppDetailsHelper(authData)
        val appDetail = detailHelper.getAppByPackageName(appPackage)
        return PurchaseHelper(authData).purchase(
            appDetail.packageName, appDetail.versionCode, appDetail.offerType
        ).map {
            DownloadItem(it.name, it.url, null, null)
        }
    }

    private fun getAuthJson(hub: HubData): Map<String, String?>? {
        return hub.auth[EMAIL_AUTH]?.let {
            hub.auth[TOKEN_AUTH]?.run {
                mapOf(
                    EMAIL_AUTH to hub.auth[EMAIL_AUTH], TOKEN_AUTH to hub.auth[TOKEN_AUTH]
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
        authJson: Map<String, String?>, deviceName: String = "px_3a"
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