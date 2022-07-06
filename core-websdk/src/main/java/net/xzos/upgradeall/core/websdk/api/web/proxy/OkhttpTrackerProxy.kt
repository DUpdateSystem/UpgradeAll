package net.xzos.upgradeall.core.websdk.api.web.proxy

import net.xzos.upgradeall.core.websdk.api.web.HttpError
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.http.HttpResponse
import net.xzos.upgradeall.core.websdk.api.web.http.OkHttpApi
import okhttp3.Response

open class OkhttpTrackerProxy : OkhttpTimeoutProxy {
    private val cancelProxy = OkhttpCancelProxy()
    private val okHttpApi = OkHttpApi
    private val okHttpExecuteApi = OkHttpApi

    fun cancelCall(data: HttpRequestData) {
        cancelProxy.cancelCall(data)
    }

    fun shutdown() {
        // clean http request
    }

    protected fun okhttpExecuteWithTracker(
        requestData: HttpRequestData, checkRunnable: () -> Boolean = { true }
    ): Response? {
        val call = okHttpExecuteApi.getCall(requestData).apply {
            cancelProxy.register(requestData, this)
        }
        return try {
            okhttpExecuteWithTimeout(call, checkRunnable)
        } finally {
            cancelProxy.unregister(requestData)
        }
    }

    fun okhttpAsyncWithTracker(
        requestData: HttpRequestData,
        callback: (HttpResponse?) -> Unit,
        errorCallback: (HttpError) -> Unit,
        checkRunnable: () -> Boolean = { true },
    ) {
        val call = okHttpApi.getCall(requestData).apply {
            cancelProxy.register(requestData, this)
        }
        okhttpAsyncWithTimeout(call, {
            cancelProxy.cancelCall(requestData)
            callback(it)
        }, errorCallback, checkRunnable)
    }
}
