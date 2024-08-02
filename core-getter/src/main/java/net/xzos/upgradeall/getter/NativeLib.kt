package net.xzos.upgradeall.getter

import android.content.Context

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
    external fun runServer(context:Context, callback: RunServerCallback): String
    fun runServerLambda(context: Context, callback: (String) -> Unit): String {
        return runServer(context, RunServerCallback(callback))
    }

    companion object {
        // Used to load the 'getter' library on application startup.
        init {
            System.loadLibrary("api_proxy")
        }
    }
}