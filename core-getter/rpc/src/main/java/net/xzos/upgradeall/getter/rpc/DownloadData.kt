package net.xzos.upgradeall.getter.rpc

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

enum class DownloadState {
    @SerializedName("pending")
    @JsonProperty("pending")
    PENDING,

    @SerializedName("downloading")
    @JsonProperty("downloading")
    DOWNLOADING,

    @SerializedName("stopped")
    @JsonProperty("stopped")
    STOPPED,

    @SerializedName("completed")
    @JsonProperty("completed")
    COMPLETED,

    @SerializedName("failed")
    @JsonProperty("failed")
    FAILED,

    @SerializedName("cancelled")
    @JsonProperty("cancelled")
    CANCELLED
}

data class DownloadProgress(
    @SerializedName("downloaded_bytes")
    @JsonProperty("downloaded_bytes")
    val downloadedBytes: Long = 0,

    @SerializedName("total_bytes")
    @JsonProperty("total_bytes")
    val totalBytes: Long? = null,

    @SerializedName("speed_bytes_per_sec")
    @JsonProperty("speed_bytes_per_sec")
    val speedBytesPerSec: Long? = null,

    @SerializedName("eta_seconds")
    @JsonProperty("eta_seconds")
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
    @JsonProperty("task_id")
    val taskId: String = "",

    @SerializedName("url")
    @JsonProperty("url")
    val url: String = "",

    @SerializedName("dest_path")
    @JsonProperty("dest_path")
    val destPath: String = "",

    @SerializedName("state")
    @JsonProperty("state")
    val state: DownloadState = DownloadState.PENDING,

    @SerializedName("progress")
    @JsonProperty("progress")
    val progress: DownloadProgress = DownloadProgress(),

    @SerializedName("resume_offset")
    @JsonProperty("resume_offset")
    val resumeOffset: Long = 0,

    @SerializedName("supports_range")
    @JsonProperty("supports_range")
    val supportsRange: Boolean? = null,

    @SerializedName("error")
    @JsonProperty("error")
    val error: String? = null,

    @SerializedName("created_at")
    @JsonProperty("created_at")
    val createdAt: Long? = null,

    @SerializedName("started_at")
    @JsonProperty("started_at")
    val startedAt: Long? = null,

    @SerializedName("completed_at")
    @JsonProperty("completed_at")
    val completedAt: Long? = null,

    @SerializedName("paused_at")
    @JsonProperty("paused_at")
    val pausedAt: Long? = null,

    @SerializedName("headers")
    @JsonProperty("headers")
    val headers: Map<String, String>? = null,

    @SerializedName("cookies")
    @JsonProperty("cookies")
    val cookies: Map<String, String>? = null
)

data class TaskIdResponse(
    @SerializedName("task_id")
    @JsonProperty("task_id")
    val taskId: String = ""
)
