package net.xzos.upgradeall.core.websdk.web.http

class HttpRequestData(
    val url: String, val headers: Map<String, String> = mapOf(),
    val markId: String? = null
)
