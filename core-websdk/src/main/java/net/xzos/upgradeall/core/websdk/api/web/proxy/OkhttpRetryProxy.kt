package net.xzos.upgradeall.core.websdk.api.web.proxy

import net.xzos.upgradeall.core.websdk.api.web.HttpError
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.http.HttpResponse
import okhttp3.Response

internal open class OkhttpRetryProxy : OkhttpTrackerProxy() {
    protected fun okhttpExecuteWithRetry(
        requestData: HttpRequestData, checkRunnable: () -> Boolean = { true }, retryNum: Int = 3
    ): Response? {
        for (i in 0..retryNum) {
            try {
                return okhttpExecuteWithTracker(requestData, checkRunnable)
            } catch (e: WebTimeoutError) {
                continue
            }
        }
        return null
    }

    protected fun okhttpAsyncWithRetry(
        requestData: HttpRequestData,
        callback: (HttpResponse?) -> Unit,
        errorCallback: (HttpError) -> Unit,
        checkRunnable: () -> Boolean = { true },
        retryNum: Int = 3
    ) {
        okhttpAsyncWithTracker(requestData, callback, {
            if (it.error is WebTimeoutError)
                okhttpAsyncWithRetry(
                    requestData, callback, errorCallback,
                    checkRunnable, retryNum - 1
                )
            else errorCallback(it)
        }, checkRunnable)
    }
}