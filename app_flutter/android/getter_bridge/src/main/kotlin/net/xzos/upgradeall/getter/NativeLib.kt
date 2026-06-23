package net.xzos.upgradeall.getter

import android.content.Context

class RunServerCallback(private val callback: (String) -> Unit) {
    fun callback(url: String) {
        callback.invoke(url)
    }
}

class NativeLib {
    external fun runServer(context: Context, callback: RunServerCallback): String
    external fun initializeBridge(context: Context): String
    external fun previewInstalledAutogen(context: Context, requestJson: String): String
    external fun applyInstalledAutogen(requestJson: String): String

    fun runServerLambda(context: Context, callback: (String) -> Unit): String {
        return runServer(context, RunServerCallback(callback))
    }

    companion object {
        init {
            System.loadLibrary("api_proxy")
        }
    }
}
