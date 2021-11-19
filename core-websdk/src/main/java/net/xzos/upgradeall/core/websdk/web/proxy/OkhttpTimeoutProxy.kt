package net.xzos.upgradeall.core.websdk.web.proxy

import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.websdk.web.HttpError
import net.xzos.upgradeall.core.websdk.web.http.HttpResponse
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

internal interface OkhttpTimeoutProxy {
    companion object {
        private const val TAG = "OkhttpTimeoutProxy"
        private val objectTag = ObjectTag(ObjectTag.core, TAG)
    }

    fun okhttpExecuteWithTimeout(
        call: Call, checkRunnable: () -> Boolean = { true }
    ): Response? {
        if (!checkRunnable()) return null
        val response = try {
            call.execute()
        } catch (e: SocketTimeoutException) {
            throw WebTimeoutError("okhttpExecute: Client timeout: ${e.message}")
        } catch (e: Throwable) {
            throw e
        }
        if (response.code != 408) {
            return response
        } else {
            throw WebTimeoutError("okhttpExecute: Server timeout: $response")
        }
    }

    fun okhttpAsyncWithTimeout(
        call: Call,
        callback: (HttpResponse?) -> Unit,
        errorCallback: (HttpError) -> Unit,
        checkRunnable: () -> Boolean = { true },
    ) {
        if (!checkRunnable()) callback(null)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (e is SocketTimeoutException) {
                    errorCallback(
                        HttpError(
                            WebTimeoutError("doOkhttpCall: Client timeout: ${e.message}"),
                            call,
                        )
                    )
                } else {
                    errorCallback(HttpError(e, call))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 408) {
                    errorCallback(
                        HttpError(
                            WebTimeoutError("doOkhttpCall: Server timeout: $response"),
                            call
                        )
                    )
                } else {
                    callback(HttpResponse(response.code, response.body?.string()))
                }
            }
        })
    }
}