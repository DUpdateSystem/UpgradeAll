package net.xzos.upgradeall.core.utils

import org.json.JSONObject

fun JSONObject.getOrNull(key: String): String? {
    return getOrNull(key, this::getString)
}

fun <T> JSONObject.getOrNull(key: String, getFun: (String) -> T): T? {
    return if (this.has(key))
        with(getFun(key)) {
            if (this != "null")
                this
            else null
        } else null
}