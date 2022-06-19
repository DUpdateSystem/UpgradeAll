package net.xzos.upgradeall.core.websdk.api.web.http

class HttpRequestData(
    val url: String, val headers: Map<String, String> = mapOf(),
    val markId: String? = null
)
