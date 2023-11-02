package net.xzos.upgradeall.getter

class NativeLib {

    /**
     * A native method that is implemented by the 'getter' native library,
     * which is packaged with this application.
     */
    external fun check_app_available(hub_uuid: String, id_map: Map<String, String>): Boolean
    external fun get_app_releases(hub_uuid: String, id_map: Map<String, String>): String

    companion object {
        // Used to load the 'getter' library on application startup.
        init {
            System.loadLibrary("api_proxy")
        }
    }
}