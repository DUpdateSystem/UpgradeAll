package net.xzos.upgradeall.core.network_api

import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag.Companion.core
import net.xzos.upgradeall.core.route.Empty
import net.xzos.upgradeall.core.route.HttpRequestItem
import net.xzos.upgradeall.core.route.HttpResponseItem
import net.xzos.upgradeall.core.route.UpdateServerRouteGrpc


object ClientProxy {
    private const val TAG = "ClientProxy"
    private val objectTag = ObjectTag(core, TAG)
    private var httpResponse: StreamObserver<HttpResponseItem>? = null
    private var id = -1

    private var running = 120  // 每一次检查循环减一
    private var taskNum = 0  // 每一次检查循环减一
    private var stopMutex = Mutex()  // 是否手动停止程序
    private var runningMutex = Mutex()  // 单一运行锁

    private fun initRuntime() {
        running = 120
        taskNum = 0
        checkStop()
    }

    suspend fun newClientProxy(mChannel: ManagedChannel) {
        if (runningMutex.isLocked) return
        runningMutex.withLock {
            initRuntime()
            val blockingStub = UpdateServerRouteGrpc.newBlockingStub(mChannel)
            val asyncStub = UpdateServerRouteGrpc.newStub(mChannel)
            httpResponse = asyncStub.newClientProxyReturn(object : StreamObserver<Empty> {
                override fun onNext(value: Empty?) {}

                override fun onError(t: Throwable?) {
                    return
                }

                override fun onCompleted() {
                    return
                }
            })
            val httpRequestIterator = blockingStub.newClientProxyCall(Empty.newBuilder().build())
            for (httpRequest in httpRequestIterator) {
                GlobalScope.launch(Dispatchers.IO) {
                    processRequest(httpRequest)
                }
            }
            httpResponse!!.onCompleted()
            httpResponse = null
        }
    }

    private fun processRequest(httpRequest: HttpRequestItem) {
        running++
        taskNum++
        if (httpRequest.method == "id") {
            id = httpRequest.key.toInt()
            pushHttpResponse(httpResponse!!,
                    HttpResponseItem.newBuilder().setCode(0).setKey(id.toString()).build())
        } else {
            pushHttpResponse(httpResponse!!, getHttpResponseItem(httpRequest))
        }
        taskNum--
    }

    private fun checkStop() {
        if (stopMutex.isLocked) return
        GlobalScope.launch {
            stopMutex.withLock {
                while (true) {
                    delay(1000L)
                    if (running <= 0 && taskNum <= 0) {
                        httpResponse?.onCompleted()
                        break
                    }
                    running--
                }
            }
        }
    }

    private fun getHttpResponseItem(request: HttpRequestItem?): HttpResponseItem {
        request ?: return HttpResponseItem.newBuilder().setCode(0).build()
        val key = request.key
        val method = request.method
        val url = request.url
        val headers = request.headersList
        val headersMap = mutableMapOf<String, String>().apply {
            for (header in headers)
                this[header.key] = header.value
        }
        val httpResponseItemBuilder = HttpResponseItem.newBuilder().setKey(key)
        val response = when (method) {
            "get" -> OkHttpApi.get(objectTag, url, headersMap)
            "post" -> {
                val body = request.body
                OkHttpApi.post(objectTag, url, headersMap, body.type, body.text)
            }
            else -> null
        } ?: return httpResponseItemBuilder.setCode(-1).build()
        return httpResponseItemBuilder.setCode(response.code).setText(response.body?.string()).build()
    }

    private fun pushHttpResponse(httpResponse: StreamObserver<HttpResponseItem>,
                                 httpResponseItem: HttpResponseItem) {
        try {
            httpResponse.onNext(httpResponseItem)
        } catch (e: RuntimeException) {
            // Cancel RPC
            httpResponse.onError(e)
        }
    }
}
