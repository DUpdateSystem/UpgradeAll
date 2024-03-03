package net.xzos.upgradeall.getter

import android.util.Log

class GetterPort {
    private val nativeLib = NativeLib()
    fun checkAppAvailable(
        hub_uuid: String,
        app_data: Map<String, String>,
        hub_data: Map<String, String>
    ): Boolean {
        return nativeLib.checkAppAvailable(hub_uuid, app_data, hub_data)
            .also { Log.e("GetterPort", "checkAppAvailable: $it") }
    }

    fun getAppLatestRelease(
        hub_uuid: String,
        app_data: Map<String, String>,
        hub_data: Map<String, String>
    ): String {
        return String(
            nativeLib.getAppLatestRelease(hub_uuid, app_data, hub_data)
                .also { Log.e("GetterPort", "getAppLatestRelease: $it") })
    }

    fun getAppReleases(
        hub_uuid: String,
        app_data: Map<String, String>,
        hub_data: Map<String, String>
    ): String {
        return String(
            nativeLib.getAppReleases(hub_uuid, app_data, hub_data)
                .also { Log.e("GetterPort", "getAppReleases: $it") })
    }
}