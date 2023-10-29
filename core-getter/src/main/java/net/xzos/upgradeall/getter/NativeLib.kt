package net.xzos.upgradeall.getter

class NativeLib {

    /**
     * A native method that is implemented by the 'getter' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(name: String): String

    companion object {
        // Used to load the 'getter' library on application startup.
        init {
            System.loadLibrary("api_proxy")
        }
    }
}