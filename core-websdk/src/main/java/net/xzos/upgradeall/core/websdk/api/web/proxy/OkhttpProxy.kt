package net.xzos.upgradeall.core.websdk.api.web.proxy

import net.xzos.upgradeall.core.websdk.api.web.HttpError
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.http.HttpResponse

open class OkhttpProxy : OkhttpProxyCallNoErrorApi() {
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