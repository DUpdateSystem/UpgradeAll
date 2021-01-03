package net.xzos.upgradeall.core.module.network

import net.xzos.upgradeall.core.route.Dict
import java.security.MessageDigest


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
