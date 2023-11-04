package net.xzos.upgradeall.getter

class GetterPort {
    private val nativeLib = NativeLib()
    fun checkAppAvailable(hub_uuid: String, id_map: Map<String, String>): Boolean {
        return nativeLib.checkAppAvailable(hub_uuid, id_map)
    }

    fun getAppLatestRelease(hub_uuid: String, id_map: Map<String, String>): String {
        return nativeLib.getAppLatestRelease(hub_uuid, id_map)
    }
    fun getAppReleases(hub_uuid: String, id_map: Map<String, String>): String {
        return nativeLib.getAppReleases(hub_uuid, id_map)
    }
}