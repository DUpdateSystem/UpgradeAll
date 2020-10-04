package net.xzos.upgradeall.core.network_api

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.route.Dict
import java.security.MessageDigest


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
