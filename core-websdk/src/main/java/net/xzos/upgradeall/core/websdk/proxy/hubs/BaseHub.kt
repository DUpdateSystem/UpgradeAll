package net.xzos.upgradeall.core.websdk.proxy.hubs

import org.json.JSONArray
import org.json.JSONObject

abstract class BaseHub {
    open val uuid: String = ""

    open fun getRelease(appId: Map<String, String?>, auth: Map<String, String?>): JSONArray? {
        return null
    }

    fun getDownload(
        appId: Map<String, String?>,
        auth: Map<String, String?>,
        assetIndex: List<Int>
    ): JSONObject? {
        return null
    }
}