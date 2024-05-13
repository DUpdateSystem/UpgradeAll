package net.xzos.upgradeall.getter

class RunServerCallback(private val _callback: (String) -> Unit) {
    fun callback(url: String) {
        _callback(url)
    }
}

class NativeLib {

    /**
     * A native method that is implemented by the 'getter' native library,
     * which is packaged with this application.
     */
    external fun runServer(callback: RunServerCallback): String
    fun runServerLambda(callback: (String) -> Unit): String {
        return runServer(RunServerCallback(callback))
    }

    companion object {
        // Used to load the 'getter' library on application startup.
        init {
            System.loadLibrary("api_proxy")
        }
    }
}