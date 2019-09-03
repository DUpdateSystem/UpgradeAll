package net.xzos.upgradeAll.json.cache

import org.jsoup.nodes.Document

data class JSCacheData(
        val httpResponseDict: MutableMap<String, String> = mutableMapOf(),
        val jsoupDomDict: MutableMap<String, Document> = mutableMapOf()
)
