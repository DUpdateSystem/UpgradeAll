package net.xzos.upgradeall.core.websdk.web.proxy

import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.websdk.web.http.HttpRequestData
import okhttp3.Call

open class OkhttpCancelProxy {
    private val callMap = coroutinesMutableMapOf<HttpRequestData, Call>()

    fun register(data: HttpRequestData, call: Call) {
        callMap[data] = call
    }

    fun unregister(data: HttpRequestData) {
        callMap.remove(data)
    }

    fun cancelCall(data: HttpRequestData) {
        callMap.remove(data)?.cancel()
    }
}