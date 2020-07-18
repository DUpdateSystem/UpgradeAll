package net.xzos.upgradeall.core.network_api

import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag.Companion.core
import net.xzos.upgradeall.core.route.*


object ClientProxy {
    private const val TAG = "ClientProxy"
    private val objectTag = ObjectTag(core, TAG)

    fun processRequest(hubUuid: String, appId: List<AppIdItem>, response: Response): Request? {
        if (!response.needProxy) return null
        val httpResponseItem = getHttpResponseItem(response.httpProxyRequest)
        return Request.newBuilder().setHubUuid(hubUuid).addAllAppId(appId)
                .setFunId(response.nextFunId).setHttpResponse(httpResponseItem).build()
    }

    private fun getHttpResponseItem(request: HttpRequestItem): HttpResponseItem {
        val method = request.method
        val url = request.url
        val headers = request.headersList
        val headersMap = mutableMapOf<String, String>().apply {
            for (header in headers)
                this[header.key] = header.value
        }
        val httpResponseItemBuilder = HttpResponseItem.newBuilder()
        val response = when (method) {
            "get" -> OkHttpApi.get(objectTag, url, headersMap)
            "post" -> {
                val body = request.body
                OkHttpApi.post(objectTag, url, headersMap, body.type, body.text)
            }
            else -> null
        }
        response ?: return httpResponseItemBuilder.setStatusCode(-1).build()
        return httpResponseItemBuilder.setStatusCode(response.code).setText(response.body?.string()).build()
    }
}
