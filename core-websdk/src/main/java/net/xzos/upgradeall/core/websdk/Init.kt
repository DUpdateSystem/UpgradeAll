package net.xzos.upgradeall.core.websdk

import android.content.Context
import net.xzos.upgradeall.core.utils.data_cache.CacheConfig
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.websdk.api.ServerApi
import net.xzos.upgradeall.core.websdk.api.ServerApiProxy
import net.xzos.upgradeall.core.websdk.api.client_proxy.hubs.CoolApk
import net.xzos.upgradeall.core.websdk.api.client_proxy.hubs.GooglePlay
import net.xzos.upgradeall.core.websdk.api.client_proxy.rpc.KotlinHubRpcServer
import net.xzos.upgradeall.core.websdk.api.client_proxy.rpc.KotlinDownloaderRpcServer
import net.xzos.upgradeall.core.websdk.api.client_proxy.rpc.GooglePlayDownloaderImpl
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.getter.GetterPort
import net.xzos.upgradeall.getter.RustConfig
import net.xzos.upgradeall.getter.runGetterServer
import java.io.File

class Data(
    var serverApi: ServerApi? = null,
    var getterPort: GetterPort? = null,
)

val data = Data()
val serverApi get() = data.serverApi
val getterPort get() = data.getterPort!!
lateinit var dataCacheManager: DataCacheManager

private var kotlinHubRpcServer: KotlinHubRpcServer? = null
private var kotlinDownloaderRpcServer: KotlinDownloaderRpcServer? = null
private var googlePlayDownloaderImpl: GooglePlayDownloaderImpl? = null

fun initSdkCache(config: CacheConfig) {
    dataCacheManager = DataCacheManager(config)
}

fun initRustSdkApi(
    dataDir: File,
    cacheDir: File,
    globalExpireTime: Long,
) {
    with(data) {
        getterPort = GetterPort(RustConfig(cacheDir, dataDir, globalExpireTime))
    }
}

suspend fun runGetterService(context: Context) {
    runGetterServer(context, getterPort)
    getterPort.init()

    // Start Kotlin Hub RPC Server and register Kotlin hubs as OutsideProviders in the Rust getter
    val okhttpProxy = OkhttpProxy()
    val hubs =
        mapOf(
            "65c2f60c-7d08-48b8-b4ba-ac6ee924f6fa" to GooglePlay(dataCacheManager, okhttpProxy),
            "1c010cc9-cff8-4461-8993-a86cd190d377" to CoolApk(dataCacheManager, okhttpProxy),
        )
    val server = KotlinHubRpcServer(hubs)
    val kotlinRpcUrl = server.start()
    kotlinHubRpcServer = server

    // Register each Kotlin hub with the Rust getter via JSON-RPC
    server.getHubUuids().forEach { uuid ->
        getterPort.registerProvider(uuid, kotlinRpcUrl)
    }

    // Start Google Play Downloader RPC Server and register it with Rust getter
    val googlePlayDownloader = GooglePlayDownloaderImpl(getterPort.getService())
    googlePlayDownloaderImpl = googlePlayDownloader
    val downloaderServer = KotlinDownloaderRpcServer(googlePlayDownloader)
    val downloaderRpcUrl = downloaderServer.start()
    kotlinDownloaderRpcServer = downloaderServer

    // Register Google Play downloader with the Rust getter via JSON-RPC
    // UUID matches GooglePlay hub UUID
    getterPort.registerDownloader("65c2f60c-7d08-48b8-b4ba-ac6ee924f6fa", downloaderRpcUrl)
}

fun renewSdkApi(host: String) {
    with(data) {
        serverApi?.shutdown()
        serverApi = ServerApi(host, dataCacheManager)
    }
}

fun getServerApi() = ServerApiProxy { serverApi }
