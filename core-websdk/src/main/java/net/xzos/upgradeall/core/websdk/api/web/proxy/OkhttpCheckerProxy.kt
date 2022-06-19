package net.xzos.upgradeall.core.websdk.api.web.proxy

import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.websdk.api.web.HttpError
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.http.HttpResponse
import okhttp3.Response

internal open class OkhttpCheckerProxy : OkhttpRetryProxy() {
    private val invalidMarkList = coroutinesMutableListOf<String>(true)

    protected fun okhttpExecuteWithChecker(
        requestData: HttpRequestData, retryNum: Int = 3
    ): Response? {
        val response = okhttpExecuteWithRetry(requestData, requestData.toCheckRunnable(), retryNum)
        requestData.markId?.apply {
            if (response?.code == 400) invalidMarkList.add(this)
        }
        return response
    }

    protected fun okhttpAsyncWithChecker(
        requestData: HttpRequestData,
        callback: (HttpResponse?) -> Unit,
        errorCallback: (HttpError) -> Unit,
        retryNum: Int = 3
    ) {
        okhttpAsyncWithRetry(
            requestData, {
                requestData.markId?.apply {
                    if (it?.code == 400) invalidMarkList.add(this)
                }
                callback(it)
            },
            errorCallback, requestData.toCheckRunnable(), retryNum
        )
    }

    private fun HttpRequestData.toCheckRunnable(): () -> Boolean =
        { this.markId?.let { it !in invalidMarkList } ?: true }
}