package net.xzos.upgradeall.core.data.json

import com.google.gson.annotations.SerializedName


const val FOREVER_IGNORE = "FOREVER_IGNORE"

class IgnoreApp private constructor(
        @SerializedName("package_id") var packageId: Map<String, String>,
        @SerializedName("version_number") var versionNumber: String,
)

fun isForeverIgnore(versionNumber: String) = versionNumber == FOREVER_IGNORE
