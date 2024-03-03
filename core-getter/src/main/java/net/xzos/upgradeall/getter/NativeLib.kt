package net.xzos.upgradeall.getter

class NativeLib {

    /**
     * A native method that is implemented by the 'getter' native library,
     * which is packaged with this application.
     */
    external fun checkAppAvailable(
        hub_uuid: String,
        app_data: Map<String, String>,
        hub_data: Map<String, String>
    ): Boolean

    external fun getAppLatestRelease(
        hub_uuid: String,
        app_data: Map<String, String>,
        hub_data: Map<String, String>
    ): ByteArray

    external fun getAppReleases(
        hub_uuid: String,
        app_data: Map<String, String>,
        hub_data: Map<String, String>
    ): ByteArray

    companion object {
        // Used to load the 'getter' library on application startup.
        init {
            System.loadLibrary("api_proxy")
        }
    }
}