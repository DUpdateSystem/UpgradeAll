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
    external fun runServer(context: Context, callback: RunServerCallback): String
    external fun initializeBridge(context: Context): String
    external fun previewInstalledAutogen(context: Context, requestJson: String): String
    external fun applyInstalledAutogen(requestJson: String): String
    external fun importLegacyRoomDatabase(requestJson: String): String
    external fun legacyReportList(requestJson: String): String
    external fun runtimeOperation(requestJson: String): String
    external fun drainRuntimeNotifications(): String

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