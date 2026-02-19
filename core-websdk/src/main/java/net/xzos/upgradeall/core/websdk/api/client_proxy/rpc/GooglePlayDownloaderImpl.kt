package net.xzos.upgradeall.core.websdk.api.client_proxy.rpc

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.getter.rpc.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Google Play downloader implementation that:
 * 1. Uses GetterService's default HTTP downloader for actual downloads
 * 2. Implements the DownloaderImpl interface for RPC exposure
 * 
 * This creates a bridge: GooglePlay URLs -> HTTP download via Rust's TraumaDownloader
 */
class GooglePlayDownloaderImpl(
    private val getterService: GetterService,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : DownloaderImpl {
    
    // Map external task_id -> internal task_id (from Rust downloader)
    private val taskMapping = ConcurrentHashMap<String, String>()
    
    // Map external task_id -> RustDownloader instance
    private val downloaders = ConcurrentHashMap<String, RustDownloader>()
    
    // Mutex for task operations
    private val taskMutex = Mutex()

    override suspend fun submitDownload(
        url: String,
        destPath: String,
        headers: Map<String, String>,
        cookies: Map<String, String>
    ): String {
        val externalTaskId = UUID.randomUUID().toString()
        
        return taskMutex.withLock {
            try {
                // Create RustDownloader with hubUuid=null to use default HTTP downloader
                // (Google Play URLs are already plain HTTP/HTTPS download URLs)
                val downloader = RustDownloaderBuilder(getterService)
                    .url(url)
                    .destPath(destPath)
                    .headers(headers)
                    .cookies(cookies)
                    .hubUuid(null)  // Use default HTTP downloader, not recursive
                    .scope(scope)
                    .build()
                
                // Start the download
                downloader.start()
                
                // Store the downloader
                downloaders[externalTaskId] = downloader
                
                // Wait a bit for the Rust task_id to be available
                // In real scenario, we'd get this from the downloader
                // For now, use the external ID as mapping
                taskMapping[externalTaskId] = externalTaskId
                
                externalTaskId
            } catch (e: Exception) {
                throw RuntimeException("Failed to submit download: ${e.message}", e)
            }
        }
    }

    override suspend fun getStatus(taskId: String): TaskInfo? {
        val downloader = downloaders[taskId] ?: return null
        return convertToTaskInfo(taskId, downloader)
    }

    override suspend fun waitForChange(taskId: String, timeoutSeconds: Long): TaskInfo? {
        val downloader = downloaders[taskId] ?: return null
        
        // Wait for status change with timeout
        return withTimeoutOrNull(timeoutSeconds * 1000) {
            val initialStatus = downloader.status.value
            // Wait until status changes
            downloader.status.first { it != initialStatus }
            convertToTaskInfo(taskId, downloader)
        } ?: convertToTaskInfo(taskId, downloader)  // Return current status on timeout
    }

    override suspend fun pause(taskId: String): Boolean {
        val downloader = downloaders[taskId] ?: return false
        downloader.pause()
        return true
    }

    override suspend fun resume(taskId: String): Boolean {
        val downloader = downloaders[taskId] ?: return false
        downloader.resume()
        return true
    }

    override suspend fun cancel(taskId: String): Boolean {
        val downloader = downloaders[taskId] ?: return false
        downloader.cancel()
        
        // Clean up after cancellation
        scope.launch {
            delay(1000)
            downloaders.remove(taskId)
            taskMapping.remove(taskId)
        }
        
        return true
    }

    private fun convertToTaskInfo(taskId: String, downloader: RustDownloader): TaskInfo {
        val status = downloader.status.value
        val progress = downloader.progress.value
        
        return TaskInfo(
            taskId = taskId,
            url = "",  // URL not exposed by RustDownloader
            destPath = "",  // destPath not exposed by RustDownloader
            state = mapStatusToState(status),
            progress = DownloadProgress(
                downloadedBytes = progress.downloadedBytes,
                totalBytes = progress.totalBytes,
                speedBytesPerSec = progress.speed,
                etaSeconds = progress.eta
            ),
            error = progress.error
        )
    }

    private fun mapStatusToState(status: RustDownloadStatus): DownloadState {
        return when (status) {
            RustDownloadStatus.NONE -> DownloadState.PENDING
            RustDownloadStatus.START -> DownloadState.PENDING
            RustDownloadStatus.RUNNING -> DownloadState.DOWNLOADING
            RustDownloadStatus.STOP -> DownloadState.STOPPED
            RustDownloadStatus.COMPLETE -> DownloadState.COMPLETED
            RustDownloadStatus.FAIL -> DownloadState.FAILED
            RustDownloadStatus.CANCEL -> DownloadState.CANCELLED
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        downloaders.values.forEach { it.cleanup() }
        downloaders.clear()
        taskMapping.clear()
    }
}
