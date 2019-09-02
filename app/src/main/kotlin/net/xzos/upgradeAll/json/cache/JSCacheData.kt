package net.xzos.upgradeAll.json.cache

import org.json.JSONObject

data class JSCacheData(
        val jsoupDomDict: JSONObject = JSONObject(),
        val httpResponseDict: JSONObject = JSONObject()
)
