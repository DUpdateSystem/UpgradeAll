package net.xzos.upgradeall.core.websdk.api.web.proxy

import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.websdk.api.web.HttpError
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.http.HttpResponse
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
        errorCallback: (HttpError) -> Unit,
        retryNum: Int = 3
    ) {
        try {
            okhttpAsyncWithChecker(
                requestData, callback,
                {
                    Log.e(
                        objectTag, TAG,
                        "doOkhttpCall: url: ${it.call?.request()?.url}, e: ${it.error.stackTraceToString()}"
                    )
                    errorCallback(it)
                }, retryNum
            )
        }catch (e:Throwable){
            errorCallback(HttpError(e))
        }
    }
}