package net.xzos.upgradeall.core.network_api

import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag.Companion.core
import net.xzos.upgradeall.core.route.HttpProxyResponse
import net.xzos.upgradeall.core.route.HttpRequestItem
import net.xzos.upgradeall.core.route.HttpResponseItem
import net.xzos.upgradeall.core.route.Response


object ClientProxy {
    private const val TAG = "ClientProxy"
    private val objectTag = ObjectTag(core, TAG)

    fun processRequest(response: Response): HttpProxyResponse? {
        if (!needHttpProxy(response)) return null
        val httpResponse = response.httpProxy
        val httpResponseItem = getHttpResponseItem(httpResponse.httpProxyRequest)
        return HttpProxyResponse.newBuilder()
                .setFunId(httpResponse.nextFunId)
                .setHttpResponse(httpResponseItem)
                .build()
    }

    fun needHttpProxy(response: Response?): Boolean =
            response != null && response.hasHttpProxy()

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
