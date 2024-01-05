package net.xzos.upgradeall.getter

import android.util.Log

class GetterPort {
    private val nativeLib = NativeLib()
    fun checkAppAvailable(hub_uuid: String, id_map: Map<String, String>): Boolean {
        return nativeLib.checkAppAvailable(hub_uuid, id_map)
            .also { Log.e("GetterPort", "checkAppAvailable: $it") }
    }

    fun getAppLatestRelease(hub_uuid: String, id_map: Map<String, String>): String {
        return String(nativeLib.getAppLatestRelease(hub_uuid, id_map))
            .also { Log.e("GetterPort", "getAppLatestRelease: $it") }
    }

    fun getAppReleases(hub_uuid: String, id_map: Map<String, String>): String {
        return String(nativeLib.getAppReleases(hub_uuid, id_map))
            .also { Log.e("GetterPort", "getAppReleases: $it") }
    }
}