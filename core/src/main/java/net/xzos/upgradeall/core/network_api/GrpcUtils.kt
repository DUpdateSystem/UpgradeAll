package net.xzos.upgradeall.core.network_api

import net.xzos.upgradeall.core.route.Dict

fun gRPCDictToMap(dictList: List<Dict>): Map<String, String> {
    val map = mutableMapOf<String, String>()
    for (dict in dictList) {
        map[dict.key] = dict.value
    }
    return map
}

fun mapTogRPCDictTo(map: Map<String, String?>): List<Dict> {
    return map.map {
        Dict.newBuilder().setKey(it.key).setValue(it.value).build()
    }
}
