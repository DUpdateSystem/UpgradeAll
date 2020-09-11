package net.xzos.upgradeall.core.network_api

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.route.Dict
import java.security.MessageDigest

internal class HubData(
        val hubUuid: String,
        val auth: Map<String, String?> = mapOf()
) {
    private val appIdList: HashSet<Map<String, String?>> = hashSetOf()
    private val dataMutex = Mutex()

    fun addAppId(appId: Map<String, String?>) {
        runBlocking {
            dataMutex.withLock {
                appIdList.add(appId)
            }
        }
    }

    fun getAppIdList(): List<Map<String, String?>> {
        return runBlocking {
            dataMutex.withLock {
                appIdList.toList()
            }
        }
    }
}


fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    md.update(this.toByteArray())
    return md.digest().toString(Charsets.UTF_8)
}

fun List<Dict>.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    for (dict in this) {
        map[dict.k] = dict.v
    }
    return map
}

fun Map<String, String?>.togRPCDict(): List<Dict> {
    return this.map {
        Dict.newBuilder().setK(it.key).setV(it.value).build()
    }
}
