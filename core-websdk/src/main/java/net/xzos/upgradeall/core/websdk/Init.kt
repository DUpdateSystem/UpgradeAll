package net.xzos.upgradeall.core.websdk

import net.xzos.upgradeall.core.utils.data_cache.CacheConfig
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.websdk.api.ServerApi
import net.xzos.upgradeall.core.websdk.api.ServerApiProxy

class Data(
    var serverApi: ServerApi? = null
)

val data = Data()
val serverApi get() = data.serverApi
lateinit var dataCacheManager: DataCacheManager

fun initSdkCache(config: CacheConfig) {
    dataCacheManager = DataCacheManager(config)
}

fun renewSdkApi(host: String) {
    with(data) {
        serverApi?.shutdown()
        serverApi = ServerApi(host, dataCacheManager)
    }
}

fun getServerApi() = ServerApiProxy { serverApi }