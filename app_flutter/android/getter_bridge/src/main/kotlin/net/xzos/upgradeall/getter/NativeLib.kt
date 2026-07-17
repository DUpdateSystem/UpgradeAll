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
    external fun previewInstalledFdroidAutogen(context: Context, requestJson: String): String
    external fun applyInstalledAutogen(requestJson: String): String
    external fun previewFreshInstallSetup(context: Context, requestJson: String): String
    external fun applyFreshInstallSetup(requestJson: String): String
    external fun applyInstalledFdroidAutogen(requestJson: String): String
    external fun previewGithubAutogen(context: Context, requestJson: String): String
    external fun applyGithubAutogen(requestJson: String): String
    external fun previewFdroidAutogen(requestJson: String): String
    external fun applyFdroidAutogen(requestJson: String): String
    external fun refreshDefaultFdroidCatalogCache(requestJson: String): String
    external fun importLegacyRoomDatabase(requestJson: String): String
    external fun legacyReportList(requestJson: String): String
    external fun startup(context: Context, requestJson: String): String
    external fun readOperation(requestJson: String): String
    external fun prepareInstall(requestJson: String): String
    external fun runtimeOperation(requestJson: String): String
    external fun drainRuntimeNotifications(): String

    fun runServerLambda(context: Context, callback: (String) -> Unit): String {
        return runServer(context, RunServerCallback(callback))
    }

    companion object {
        init {
            System.loadLibrary("api_proxy")
        }
    }
}
