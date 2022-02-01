package net.xzos.upgradeall.core.websdk

class Data(
    var serverApi: ServerApi? = null
)

val data = Data()
val serverApi get() = data.serverApi

fun renewServerApi(host: String, dataCacheTimeSec: Int) {
    with(data) {
        serverApi?.shutdown()
        serverApi = ServerApi(host, dataCacheTimeSec)
    }
}

fun getServerApi() = ServerApiProxy { serverApi }