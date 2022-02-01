package net.xzos.upgradeall.core.websdk.web.proxy

import net.xzos.upgradeall.core.websdk.web.HttpError
import net.xzos.upgradeall.core.websdk.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.web.http.HttpResponse
import okhttp3.Call
import okhttp3.Response

internal open class OkhttpProxy : OkhttpProxyCallNoErrorApi() {
    fun okhttpExecute(
        requestData: HttpRequestData, retryNum: Int = 3
    ) = okhttpExecuteNoError(requestData, retryNum)

    fun okhttpAsync(
        requestData: HttpRequestData,
        callback: (HttpResponse?) -> Unit,
        errorCallback: (HttpError) -> Unit = { callback(null) },
        retryNum: Int = 3
    ) = okhttpAsyncNoError(requestData, callback, errorCallback, retryNum)
}