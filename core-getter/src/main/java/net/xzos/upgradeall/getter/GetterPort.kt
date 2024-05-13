package net.xzos.upgradeall.getter

import android.util.Log
import com.googlecode.jsonrpc4j.JsonRpcHttpClient
import com.googlecode.jsonrpc4j.ProxyUtil
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.websdk.data.json.ReleaseGson
import java.net.URL


class GetterPort(private val config: RustConfig) {
    private lateinit var service: GetterService
    private val mutex = Mutex()
    private var isInit = false

    fun runService() {
        NativeLib().runServerLambda {
            initService(it)
        }.also {
            if (it.isEmpty()) {
                Log.d("GetterPort", "runService: success")
            } else {
                Log.e("GetterPort", "runService(error): $it")
            }
        }
    }

    suspend fun isInit(): Boolean {
        mutex.withLock {
            return isInit
        }
    }

    private fun initService(url: String) {
        val client = JsonRpcHttpClient(URL(url))

        this.service = ProxyUtil.createClientProxy(
            javaClass.classLoader,
            GetterService::class.java,
            client
        )
    }

    fun init(): Boolean {
        return runBlocking {
            return@runBlocking mutex.withLock {
                if (isInit) return@withLock true
                val dataPath = config.dataDir.toString()
                val cachePath = config.cacheDir.toString()
                val globalExpireTime = config.globalExpireTime
                return@withLock service.init(dataPath, cachePath, globalExpireTime)
                    .apply { isInit = this }
                    .also { Log.d("GetterPort", "checkInit: $it") }
            }
        }
    }

    fun checkAppAvailable(
        hubUuid: String, appData: Map<String, String>, hubData: Map<String, String>
    ): Boolean? {
        if (!init()) return null
        return init() && service.checkAppAvailable(hubUuid, appData, hubData)
            .also { Log.d("GetterPort", "checkAppAvailable: $it") }
    }

    fun getAppLatestRelease(
        hubUuid: String, appData: Map<String, String>, hubData: Map<String, String>
    ): ReleaseGson? {
        if (!init()) return null
        return service.getAppLatestRelease(hubUuid, appData, hubData)
            .also { Log.d("GetterPort", "getAppLatestRelease: $it") }
    }

    fun getAppReleases(
        hubUuid: String, appData: Map<String, String>, hubData: Map<String, String>
    ): List<ReleaseGson>? {
        if (!init()) return null
        return service.getAppReleases(hubUuid, appData, hubData)
            .also { Log.d("GetterPort", "getAppReleases: $it") }
    }
}