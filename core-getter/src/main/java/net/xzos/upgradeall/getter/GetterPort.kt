package net.xzos.upgradeall.getter

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.getter.rpc.GetterService
import net.xzos.upgradeall.getter.rpc.getClient
import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.ReleaseGson


class GetterPort(private val config: RustConfig) {
    private lateinit var service: GetterService
    
    private val mutex = Mutex()
    private var isInit = false

    fun runService(context: Context) {
        NativeLib().runServerLambda(context) { url ->
            service = getClient(url)
        }.also {
            if (it.isEmpty()) {
                Log.d("GetterPort", "runService: success")
            } else {
                Log.e("GetterPort", "runService(error): $it")
            }
        }
    }

    private fun isServiceRunning(): Boolean {
        return ::service.isInitialized
    }

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
            if (isServiceRunning().apply {
                    Log.d("GetterPort", "waitService: isServiceRunning $this")
                }
            ) break
            delay(1000L)
        }
        return isServiceRunning()
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
            return@withLock service.init(dataPath, cachePath, globalExpireTime)
                .apply { isInit = this }
                .also { Log.d("GetterPort", "initInternal: $it") }
        }
    }

    fun ping(): Boolean {
        if (!init()) return false
        return try {
            runBlocking {
                service.ping()
                    .also { Log.d("GetterPort", "ping: $it") }
                    .isNotEmpty()
            }
        } catch (e: Exception) {
            Log.e("GetterPort", "ping: $e")
            false
        }
    }

    fun init(): Boolean {
        return runBlocking { initInternal() }
    }

    fun shutdownService() {
        runBlocking {
            service.shutdown()
        }
    }

    fun checkAppAvailable(
        hubUuid: String, appData: Map<String, String>, hubData: Map<String, String>
    ): Boolean? {
        if (!init()) return null
        return runBlocking {
            service.checkAppAvailable(hubUuid, appData, hubData)
                .also { Log.d("GetterPort", "checkAppAvailable: $it") }
        }
    }

    fun getAppLatestRelease(
        hubUuid: String, appData: Map<String, String>, hubData: Map<String, String>
    ): ReleaseGson? {
        if (!init()) return null
        return runBlocking {
            service.getAppLatestRelease(hubUuid, appData, hubData)
                .also { Log.d("GetterPort", "getAppLatestRelease: $it") }
        }
    }

    fun getAppReleases(
        hubUuid: String, appData: Map<String, String>, hubData: Map<String, String>
    ): List<ReleaseGson>? {
        if (!init()) return null
        return runBlocking {
            service.getAppReleases(hubUuid, appData, hubData)
                .also { Log.d("GetterPort", "getAppReleases: $it") }
        }
    }

    fun getCloudConfig(apiUrl: String): CloudConfigList? {
        if (!init()) return null
        return runBlocking {
            service.getCloudConfig(apiUrl)
                .also { Log.d("GetterPort", "getCloudConfig: $it") }
        }
    }
}
