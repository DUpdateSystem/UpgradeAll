package net.xzos.upgradeall.core.downloader.filedownloader.item

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.downloader.filedownloader.item.data.TaskData
import net.xzos.upgradeall.core.downloader.filedownloader.item.data.file
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.downloader
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.utils.oberver.InformerNoTag

internal class DownloadWorker(
    val taskData: TaskData,
) : InformerNoTag<TaskSnap>() {
    private var client: HttpClient? = null
    private var job: Job? = null

    private var acceptRanges: Boolean? = null

    fun start() {
        client = HttpClient {
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = taskData.autoRetryMaxAttempts)
                exponentialDelay()
            }
        }

        resume()
    }

    fun resume() {
        launchDownloader()
    }

    fun stop() {
        job?.cancel()?.also {
            notifyChanged(TaskSnap(Status.STOP))
        }
    }

    fun cancel() {
        job?.cancel()?.also {
            client?.close()
            notifyChanged(TaskSnap(Status.CANCEL))
        }
    }

    fun delete() {
        cancel()
        taskData.file().delete()
    }

    private suspend fun testDownloadRange(): Boolean {
        return acceptRanges ?: kotlin.run {
            val client = client ?: return false
            client.prepareHead(taskData.url) {
                headers {
                    append("Range", "0")
                    taskData.headers.forEach { (k, v) ->
                        append(k, v)
                    }
                }
            }.execute().status.also {
                Log.i(objectTag, TAG, "testDownloadRange: response status: $it")
            } == HttpStatusCode.PartialContent
        }.also {
            acceptRanges = it
        }
    }

    private fun launchDownloader() {
        job = GlobalScope.launch(Dispatchers.IO) {
            try {
                startDownload()
            } catch (e: Throwable) {
                Log.e(objectTag, TAG, e.msg())
                notifyChanged(TaskSnap(Status.FAIL))
            }
        }
    }

    private suspend fun startDownload() {
        notifyChanged(TaskSnap(Status.START))
        testDownloadRange()
        client!!.prepareGet(taskData.url) {
            this.headers()
        }.execute { httpResponse ->
            val totalSize = httpResponse.contentLength() ?: -1
            val channel: ByteReadChannel = httpResponse.body()
            val file = taskData.file()
            if (httpResponse.status != HttpStatusCode.PartialContent)
                file.writer()
            notifyChanged(TaskSnap(Status.RUNNING, file.length(), totalSize))
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    file.appendBytes(bytes)
                    Log.d(
                        objectTag, TAG,
                        "startDownload: Received ${file.length()} bytes from $totalSize"
                    )
                    notifyChanged(TaskSnap(Status.RUNNING, file.length(), totalSize))
                }
            }
            Log.i(
                objectTag, TAG,
                "startDownload: Finally received ${file.length()} bytes from ${httpResponse.contentLength()}"
            )
            notifyChanged(TaskSnap(Status.COMPLETE, file.length(), totalSize))
        }
    }

    private fun HttpRequestBuilder.headers() {
        headers {
            if (acceptRanges!!) {
                append("Range", "bytes=${taskData.file().length()}")
            }
            taskData.headers.forEach { (k, v) ->
                append(k, v)
            }
        }
    }

    companion object {
        private const val TAG = "DownloadWorker"
        private val objectTag = ObjectTag(downloader, TAG)
    }
}