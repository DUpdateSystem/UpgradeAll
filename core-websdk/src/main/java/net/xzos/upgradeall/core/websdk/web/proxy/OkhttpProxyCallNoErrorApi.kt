package net.xzos.upgradeall.core.websdk.web.proxy

import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.web.http.HttpResponse
import okhttp3.Call
import okhttp3.Response

internal open class OkhttpProxyCallNoErrorApi : OkhttpCheckerProxy() {
    companion object {
        private const val TAG = "OkhttpProxyCallNoErrorApi"
        private val objectTag = ObjectTag(ObjectTag.core, TAG)
    }

    protected fun okhttpExecuteNoError(
        requestData: HttpRequestData, retryNum: Int = 3
    ): Response? = try {
        okhttpExecuteWithChecker(requestData, retryNum)
    } catch (e: Throwable) {
        Log.e(objectTag, TAG, "okhttpExecute: ${e.msg()}")
        null
    }

    protected fun okhttpAsyncNoError(
        requestData: HttpRequestData,
        callback: (HttpResponse?) -> Unit,
        errorCallback: (Call, Throwable) -> Unit,
        retryNum: Int = 3
    ) {
        okhttpAsyncWithChecker(
            requestData, callback,
            { call, e ->
                Log.e(
                    objectTag, TAG,
                    "doOkhttpCall: url: ${call.request().url}, e: ${e.stackTraceToString()}"
                )
                errorCallback(call, e)
            }, retryNum
        )
    }

}