package net.xzos.upgradeall.getter.rpc

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Rust-based downloader using RPC backend
 *
 * This downloader delegates all download operations to the Rust core via RPC,
 * providing a Kotlin-friendly interface compatible with the app's architecture.
 */
class RustDownloader(
    private val getterService: GetterService,
    private val url: String,
    private val destPath: String,
    private val headers: Map<String, String>? = null,
    private val cookies: Map<String, String>? = null,
    private val scope: CoroutineScope = GlobalScope
) {
    private var taskId: String? = null
    private val _status = MutableStateFlow(RustDownloadStatus.NONE)
    private val _progress = MutableStateFlow(RustDownloadProgress())

    val status: StateFlow<RustDownloadStatus> = _status
    val progress: StateFlow<RustDownloadProgress> = _progress

    private var pollingJob: Job? = null
    private var isStarted = false

    /**
     * Start the download task
     */
    fun start() {
        if (isStarted) return
        isStarted = true

        scope.launch {
            try {
                _status.value = RustDownloadStatus.START

                // Submit download task
                val response = getterService.downloadSubmit(url, destPath, headers, cookies)
                taskId = response.taskId

                // Start polling for updates
                startPolling()
            } catch (e: Exception) {
                // Set error before status to avoid race condition
                _progress.value = _progress.value.copy(
                    error = "Submit failed: ${e.javaClass.simpleName}: ${e.message}"
                )
                _status.value = RustDownloadStatus.FAIL
            }
        }
    }

    /**
     * Cancel the download task
     */
    fun cancel() {
        scope.launch {
            try {
                taskId?.let {
                    getterService.downloadCancel(it)
                    _status.value = RustDownloadStatus.CANCEL
                }
                stopPolling()
            } catch (e: Exception) {
                // Ignore cancellation errors
            }
        }
    }

    /**
     * Pause the download task
     */
    fun pause() {
        scope.launch {
            try {
                taskId?.let {
                    getterService.downloadPause(it)
                    _status.value = RustDownloadStatus.STOP
                }
            } catch (e: Exception) {
                // Ignore pause errors
            }
        }
    }

    /**
     * Resume a paused download task
     */
    fun resume() {
        scope.launch {
            try {
                taskId?.let {
                    getterService.downloadResume(it)
                    _status.value = RustDownloadStatus.RUNNING
                    // Restart polling to track resumed download
                    startPolling()
                }
            } catch (e: Exception) {
                _status.value = RustDownloadStatus.FAIL
                _progress.value = _progress.value.copy(error = e.message)
            }
        }
    }

    /**
     * Retry the download (start a new task)
     */
    fun retry() {
        reset()
        start()
    }

    private fun reset() {
        stopPolling()
        taskId = null
        isStarted = false
        _status.value = RustDownloadStatus.NONE
        _progress.value = RustDownloadProgress()
    }

    private fun startPolling() {
        stopPolling()

        pollingJob = scope.launch {
            val currentTaskId = taskId ?: return@launch

            while (isActive) {
                try {
                    // Use long polling to wait for state changes
                    val taskInfo = getterService.downloadWaitForChange(currentTaskId, 30)

                    updateFromTaskInfo(taskInfo)

                    // If task is in terminal state, stop polling
                    if (isTerminalState(taskInfo.state)) {
                        break
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // On error, try regular status check
                    try {
                        val taskInfo = getterService.downloadGetStatus(currentTaskId)
                        updateFromTaskInfo(taskInfo)

                        if (isTerminalState(taskInfo.state)) {
                            break
                        }
                    } catch (statusError: Exception) {
                        // If status check also fails, mark as failed
                        // Set error before status to avoid race condition
                        _progress.value = _progress.value.copy(
                            error = "Poll failed: ${statusError.javaClass.simpleName}: ${statusError.message}"
                        )
                        _status.value = RustDownloadStatus.FAIL
                        break
                    }

                    // Wait a bit before next poll
                    delay(1000)
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun updateFromTaskInfo(taskInfo: TaskInfo) {
        // Map Rust state to local state
        _status.value = when (taskInfo.state) {
            DownloadState.PENDING -> RustDownloadStatus.START
            DownloadState.DOWNLOADING -> RustDownloadStatus.RUNNING
            DownloadState.STOPPED -> RustDownloadStatus.STOP
            DownloadState.COMPLETED -> RustDownloadStatus.COMPLETE
            DownloadState.FAILED -> RustDownloadStatus.FAIL
            DownloadState.CANCELLED -> RustDownloadStatus.CANCEL
        }

        // Update progress
        _progress.value = RustDownloadProgress(
            downloadedBytes = taskInfo.progress.downloadedBytes,
            totalBytes = taskInfo.progress.totalBytes,
            speed = taskInfo.progress.speedBytesPerSec,
            eta = taskInfo.progress.etaSeconds,
            error = taskInfo.error
        )
    }

    private fun isTerminalState(state: DownloadState): Boolean {
        return state == DownloadState.COMPLETED ||
               state == DownloadState.FAILED ||
               state == DownloadState.CANCELLED
    }

    fun cleanup() {
        stopPolling()
    }
}

enum class RustDownloadStatus {
    NONE,
    START,
    RUNNING,
    STOP,
    COMPLETE,
    CANCEL,
    FAIL
}

data class RustDownloadProgress(
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val speed: Long? = null,
    val eta: Long? = null,
    val error: String? = null
) {
    fun percentage(): Float {
        return totalBytes?.let {
            if (it == 0L) 0f else (downloadedBytes.toFloat() / it) * 100f
        } ?: 0f
    }
}

/**
 * Builder for RustDownloader
 */
class RustDownloaderBuilder(private val getterService: GetterService) {
    private var url: String = ""
    private var destPath: String = ""
    private var headers: Map<String, String>? = null
    private var cookies: Map<String, String>? = null
    private var scope: CoroutineScope = GlobalScope

    fun url(url: String) = apply { this.url = url }
    fun destPath(path: String) = apply { this.destPath = path }
    fun headers(headers: Map<String, String>) = apply { this.headers = headers }
    fun cookies(cookies: Map<String, String>) = apply { this.cookies = cookies }
    fun scope(scope: CoroutineScope) = apply { this.scope = scope }

    fun build(): RustDownloader {
        require(url.isNotEmpty()) { "URL must not be empty" }
        require(destPath.isNotEmpty()) { "Destination path must not be empty" }

        // Ensure parent directory exists
        File(destPath).parentFile?.mkdirs()

        return RustDownloader(getterService, url, destPath, headers, cookies, scope)
    }
}
