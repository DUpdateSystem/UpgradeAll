package net.xzos.upgradeall.core.utils

import org.json.JSONArray
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

operator fun <T> JSONArray.iterator(): Iterator<T> =
    (0 until length()).asSequence().map {
        @Suppress("UNCHECKED_CAST")
        get(it) as T
    }.iterator()

fun <T> JSONArray.asSequence(): Sequence<T> = this.iterator<T>().asSequence()