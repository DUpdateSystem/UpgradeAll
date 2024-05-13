package net.xzos.upgradeall.core.websdk

import android.content.Context
import net.xzos.upgradeall.core.utils.data_cache.CacheConfig
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.websdk.api.ServerApi
import net.xzos.upgradeall.core.websdk.api.ServerApiProxy
import net.xzos.upgradeall.getter.GetterPort
import net.xzos.upgradeall.getter.RustConfig
import net.xzos.upgradeall.getter.runGetterWorker
import java.io.File

class Data(
    var serverApi: ServerApi? = null,
    var getterPort: GetterPort? = null,
)

val data = Data()
val serverApi get() = data.serverApi
val getterPort get() = data.getterPort!!
lateinit var dataCacheManager: DataCacheManager

fun initSdkCache(config: CacheConfig) {
    dataCacheManager = DataCacheManager(config)
}

fun initRustSdkApi(dataDir: File, cacheDir: File, globalExpireTime: Long) {
    with(data) {
        getterPort = GetterPort(RustConfig(cacheDir, dataDir, globalExpireTime))
    }
}

suspend fun runGetterService(context: Context) {
    runGetterWorker(context, getterPort)
    getterPort.init()
}

fun renewSdkApi(host: String) {
    with(data) {
        serverApi?.shutdown()
        serverApi = ServerApi(host, dataCacheManager)
    }
}

fun getServerApi() = ServerApiProxy { serverApi }