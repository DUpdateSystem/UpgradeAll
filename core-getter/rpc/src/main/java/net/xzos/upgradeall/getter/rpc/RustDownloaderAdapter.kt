package net.xzos.upgradeall.getter.rpc

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File

/**
 * Adapter to make RustDownloader compatible with existing Downloader interface
 */
class RustDownloaderAdapter(
    private val downloadDir: File,
    private val getterService: GetterService,
    private val scope: CoroutineScope
) {
    val id by lazy { hashCode() }

    private val taskWrappers: MutableList<RustTaskWrapper> = mutableListOf()
    private val pendingTasks: MutableList<RustTaskData> = mutableListOf()

    private var statusCallback: ((RustDownloadStatus) -> Unit)? = null

    fun addTask(inputData: RustInputData) {
        val destFile = File(downloadDir, inputData.name)
        pendingTasks.add(
            RustTaskData(
                name = inputData.name,
                url = inputData.url,
                destPath = destFile.absolutePath,
                headers = inputData.headers,
                cookies = inputData.cookies
            )
        )
    }

    fun getTaskList(): List<RustTaskWrapper> = taskWrappers

    fun status(): RustDownloadStatus {
        return taskWrappers.run {
            when {
                isEmpty() -> RustDownloadStatus.NONE
                any { it.snap.status == RustDownloadStatus.START } -> RustDownloadStatus.START
                any { it.snap.status == RustDownloadStatus.RUNNING } -> RustDownloadStatus.RUNNING
                all { it.snap.status == RustDownloadStatus.STOP } -> RustDownloadStatus.STOP
                all { it.snap.status == RustDownloadStatus.COMPLETE } -> RustDownloadStatus.COMPLETE
                all { it.snap.status == RustDownloadStatus.CANCEL } -> RustDownloadStatus.CANCEL
                any { it.snap.status == RustDownloadStatus.FAIL } -> RustDownloadStatus.FAIL
                else -> RustDownloadStatus.NONE
            }
        }
    }

    fun getDownloadProgress(): Long {
        var totalSize = 0L
        var downloadedSize = 0L
        taskWrappers.forEach {
            val progress = it.downloader.progress.value
            progress.totalBytes?.let { total ->
                totalSize += total
                downloadedSize += progress.downloadedBytes
            }
        }
        return if (totalSize == 0L) 0 else (downloadedSize * 100) / totalSize
    }

    fun observe(callback: (RustDownloadStatus) -> Unit) {
        statusCallback = callback
    }

    suspend fun start(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (pendingTasks.isEmpty()) {
            onError(RuntimeException("No tasks to download"))
            return
        }

        try {
            // Create RustDownloader for each task
            pendingTasks.forEach { taskData ->
                val builder = RustDownloaderBuilder(getterService)
                    .url(taskData.url)
                    .destPath(taskData.destPath)
                    .scope(scope)

                // Add headers if present
                taskData.headers?.let { builder.headers(it) }
                // Add cookies if present
                taskData.cookies?.let { builder.cookies(it) }

                val downloader = builder.build()

                val wrapper = RustTaskWrapper(
                    downloader = downloader,
                    file = File(taskData.destPath),
                    snap = RustTaskSnap(RustDownloadStatus.NONE)
                )

                taskWrappers.add(wrapper)

                // Observe status changes
                scope.launch {
                    downloader.status.collectLatest { status ->
                        val progress = downloader.progress.value
                        wrapper.snap = RustTaskSnap(
                            status = status,
                            downloadSize = progress.downloadedBytes,
                            totalSize = progress.totalBytes ?: 0,
                            speed = progress.speed,
                            eta = progress.eta,
                            error = progress.error
                        )
                        notifyStatus()
                    }
                }

                // Start download
                downloader.start()
            }

            pendingTasks.clear()
            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun pause(wrapper: RustTaskWrapper? = null) {
        wrapper?.downloader?.pause()
            ?: taskWrappers.forEach { it.downloader.pause() }
    }

    fun resume(wrapper: RustTaskWrapper? = null) {
        wrapper?.downloader?.resume()
            ?: taskWrappers.forEach { it.downloader.resume() }
    }

    fun retry(wrapper: RustTaskWrapper? = null) {
        wrapper?.downloader?.retry()
            ?: taskWrappers.forEach { it.downloader.retry() }
    }

    fun cancel(wrapper: RustTaskWrapper? = null) {
        wrapper?.downloader?.cancel()
            ?: taskWrappers.forEach { it.downloader.cancel() }
    }

    fun cleanup() {
        taskWrappers.forEach { it.downloader.cleanup() }
        taskWrappers.clear()
    }

    private fun notifyStatus() {
        statusCallback?.invoke(status())
    }
}

data class RustTaskWrapper(
    val downloader: RustDownloader,
    val file: File,
    var snap: RustTaskSnap
)

data class RustTaskSnap(
    val status: RustDownloadStatus,
    val downloadSize: Long = 0,
    val totalSize: Long = 0,
    val speed: Long? = null,
    val eta: Long? = null,
    val error: String? = null
) {
    fun progress(): Float {
        return if (totalSize <= 0L) 0f
        else (downloadSize.toFloat() / totalSize.toFloat()) * 100
    }
}

data class RustTaskData(
    val name: String,
    val url: String,
    val destPath: String,
    val headers: Map<String, String> = emptyMap(),
    val cookies: Map<String, String> = emptyMap()
)

data class RustInputData(
    val name: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val cookies: Map<String, String> = emptyMap()
)
