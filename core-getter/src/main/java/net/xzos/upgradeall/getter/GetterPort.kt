package net.xzos.upgradeall.getter

import android.util.Log

class GetterPort(val config: RustConfig) {
    private val nativeLib = NativeLib()

    fun init(): Boolean {
        val dataPath = config.dataDir.toString()
        val cachePath = config.cacheDir.toString()
        val globalExpireTime = config.globalExpireTime
        return nativeLib.init(dataPath, cachePath, globalExpireTime)
            .also { Log.d("GetterPort", "checkInit: $it") }
    }

    fun checkAppAvailable(
        hub_uuid: String, app_data: Map<String, String>, hub_data: Map<String, String>
    ): Boolean {
        return init() && nativeLib.checkAppAvailable(hub_uuid, app_data, hub_data)
            .also { Log.d("GetterPort", "checkAppAvailable: $it") }
    }

    fun getAppLatestRelease(
        hub_uuid: String, app_data: Map<String, String>, hub_data: Map<String, String>
    ): String {
        if (!init()) return String(ByteArray(0))
        return String(nativeLib.getAppLatestRelease(hub_uuid, app_data, hub_data)
            .also { Log.d("GetterPort", "getAppLatestRelease: $it") })
    }

    fun getAppReleases(
        hub_uuid: String, app_data: Map<String, String>, hub_data: Map<String, String>
    ): String {
        if (!init()) return String(ByteArray(0))
        return String(nativeLib.getAppReleases(hub_uuid, app_data, hub_data)
            .also { Log.d("GetterPort", "getAppReleases: $it") })
    }
}