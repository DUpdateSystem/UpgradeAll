package net.xzos.upgradeall.core.websdk

import net.xzos.upgradeall.core.utils.data_cache.DataCache
import java.io.File

class Data(
    var serverApi: ServerApi? = null
)

class Config

val data = Data()
val serverApi get() = data.serverApi
lateinit var dataCache: DataCache

fun initSdkCache(dataCacheTimeSec: Int, cacheDir: File) {
    dataCache = DataCache(dataCacheTimeSec)
}

fun renewSdkApi(host: String) {
    with(data) {
        serverApi?.shutdown()
        serverApi = ServerApi(host, dataCache)
    }
}

fun getServerApi() = ServerApiProxy { serverApi }