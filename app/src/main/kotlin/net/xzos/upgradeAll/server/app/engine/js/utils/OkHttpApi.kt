package net.xzos.upgradeAll.server.app.engine.js.utils

import net.xzos.upgradeAll.server.ServerContainer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object OkHttpApi {
    private val Log = ServerContainer.AppServer.log
    private const val TAG = "OkHttpApi"

    fun getHttpResponse(LogObjectTag: Array<String>, api_url: String?): String? {
        var response: Response? = null
        val client = OkHttpClient()
        val builder = Request.Builder()
        if (api_url != null) {
            builder.url(api_url)
        }
        val request = builder.build()
        try {
            response = client.newCall(request).execute()
        } catch (e: IOException) {
            Log.e(LogObjectTag, TAG, "getHttpResponse:  网络错误")
        }
        return response?.body?.string()
    }
}
