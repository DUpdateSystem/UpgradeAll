package net.xzos.upgradeall.getter

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.getter.rpc.GetterService
import net.xzos.upgradeall.getter.rpc.getClient
import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.DownloadItem
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

class GetterPort(
    private val config: RustConfig,
) {
    private lateinit var service: GetterService

    private val mutex = Mutex()
    private var isInit = false

    fun runService(context: Context) {
        NativeLib()
            .runServerLambda(context) { url ->
                service = getClient(url)
            }.also {
                if (it.isEmpty()) {
                    Log.d(logObjectTag, TAG, "runService: success")
                } else {
                    Log.e(logObjectTag, TAG, "runService error: $it")
                }
            }
    }

    private fun isServiceRunning(): Boolean = ::service.isInitialized

    /**
     * Get the GetterService instance
     */
    fun getService(): GetterService {
        if (!isServiceRunning()) {
            throw IllegalStateException("GetterService is not initialized. Call runService() first.")
        }
        return service
    }

    suspend fun waitService(): Boolean {
        while (true) {
            if (isServiceRunning()) break
            Log.d(logObjectTag, TAG, "waitService: not ready, retrying...")
            delay(1000L)
        }
        return true
    }

    /**
     * Internal suspend init - avoids nested runBlocking
     */
    private suspend fun initInternal(): Boolean {
        return mutex.withLock {
            waitService()
            if (isInit) return@withLock true
            val dataPath = config.dataDir.toString()
            val cachePath = config.cacheDir.toString()
            val globalExpireTime = config.globalExpireTime
            return@withLock service
                .init(dataPath, cachePath, globalExpireTime)
                .apply { isInit = this }
                .also { Log.i(logObjectTag, TAG, "init: $it") }
        }
    }

    fun ping(): Boolean {
        if (!init()) return false
        return try {
            runBlocking {
                service
                    .ping()
                    .isNotEmpty()
                    .also { Log.d(logObjectTag, TAG, "ping: $it") }
            }
        } catch (e: Exception) {
            Log.e(logObjectTag, TAG, "ping failed: $e")
            false
        }
    }

    fun init(): Boolean = runBlocking { initInternal() }

    fun shutdownService() {
        runBlocking {
            service.shutdown()
        }
    }

    fun checkAppAvailable(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
    ): Boolean? {
        if (!init()) return null
        return runBlocking {
            service
                .checkAppAvailable(hubUuid, appData, hubData)
                .also { Log.d(logObjectTag, TAG, "checkAppAvailable[$hubUuid]: $it") }
        }
    }

    fun getAppLatestRelease(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
    ): ReleaseGson? {
        if (!init()) return null
        return runBlocking {
            service
                .getAppLatestRelease(hubUuid, appData, hubData)
                .also { Log.d(logObjectTag, TAG, "getAppLatestRelease[$hubUuid]: ${it.versionNumber}") }
        }
    }

    fun getAppReleases(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
    ): List<ReleaseGson>? {
        if (!init()) return null
        return runBlocking {
            service
                .getAppReleases(hubUuid, appData, hubData)
                .also { Log.d(logObjectTag, TAG, "getAppReleases[$hubUuid]: ${it.size} releases") }
        }
    }

    fun getCloudConfig(apiUrl: String): CloudConfigList? {
        if (!init()) return null
        return runBlocking {
            service
                .getCloudConfig(apiUrl)
                .also { Log.d(logObjectTag, TAG, "getCloudConfig: ${it.appList.size} apps, ${it.hubList.size} hubs") }
        }
    }

    fun registerProvider(
        hubUuid: String,
        url: String,
    ): Boolean? {
        if (!init()) return null
        return runBlocking {
            service
                .registerProvider(hubUuid, url)
                .also { Log.i(logObjectTag, TAG, "registerProvider: uuid=$hubUuid url=$url result=$it") }
        }
    }

    fun registerDownloader(
        hubUuid: String,
        rpcUrl: String,
    ): Boolean? {
        if (!init()) return null
        return runBlocking {
            service
                .registerDownloader(hubUuid, rpcUrl)
                .also { Log.i(logObjectTag, TAG, "registerDownloader: uuid=$hubUuid result=$it") }
        }
    }

    fun getDownloadInfo(
        hubUuid: String,
        appData: Map<String, String>,
        hubData: Map<String, String>,
        assetIndex: List<Int>,
    ): List<DownloadItem>? {
        if (!init()) return null
        return runBlocking {
            service
                .getDownloadInfo(hubUuid, appData, hubData, assetIndex)
                .also { Log.d(logObjectTag, TAG, "getDownloadInfo[$hubUuid]: ${it.size} items") }
        }
    }

    companion object {
        private const val TAG = "GetterPort"
        private val logObjectTag = ObjectTag(core, TAG)
    }
}
