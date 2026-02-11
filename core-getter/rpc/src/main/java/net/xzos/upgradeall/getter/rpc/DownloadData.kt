package net.xzos.upgradeall.getter.rpc

import com.google.gson.annotations.SerializedName

enum class DownloadState {
    @SerializedName("pending")
    PENDING,

    @SerializedName("downloading")
    DOWNLOADING,

    @SerializedName("stopped")
    STOPPED,

    @SerializedName("completed")
    COMPLETED,

    @SerializedName("failed")
    FAILED,

    @SerializedName("cancelled")
    CANCELLED
}

data class DownloadProgress(
    @SerializedName("downloaded_bytes")
    val downloadedBytes: Long = 0,

    @SerializedName("total_bytes")
    val totalBytes: Long? = null,

    @SerializedName("speed_bytes_per_sec")
    val speedBytesPerSec: Long? = null,

    @SerializedName("eta_seconds")
    val etaSeconds: Long? = null
) {
    fun percentage(): Float? {
        return totalBytes?.let {
            if (it == 0L) 0f else (downloadedBytes.toFloat() / it) * 100f
        }
    }
}

data class TaskInfo(
    @SerializedName("task_id")
    val taskId: String = "",

    @SerializedName("url")
    val url: String = "",

    @SerializedName("dest_path")
    val destPath: String = "",

    @SerializedName("state")
    val state: DownloadState = DownloadState.PENDING,

    @SerializedName("progress")
    val progress: DownloadProgress = DownloadProgress(),

    @SerializedName("resume_offset")
    val resumeOffset: Long = 0,

    @SerializedName("supports_range")
    val supportsRange: Boolean? = null,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("created_at")
    val createdAt: Long? = null,

    @SerializedName("started_at")
    val startedAt: Long? = null,

    @SerializedName("completed_at")
    val completedAt: Long? = null,

    @SerializedName("paused_at")
    val pausedAt: Long? = null,

    @SerializedName("headers")
    val headers: Map<String, String>? = null,

    @SerializedName("cookies")
    val cookies: Map<String, String>? = null
)

data class TaskIdResponse(
    @SerializedName("task_id")
    val taskId: String = ""
)
