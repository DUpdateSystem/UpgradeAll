package net.xzos.upgradeall.core.utils

import org.json.JSONObject

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
fun JSONObject.getOrNull(key: String): String? {
    return if (this.has(key))
        with(this.getString(key)) {
            if (this != "null")
                this
            else null
        } else null
}