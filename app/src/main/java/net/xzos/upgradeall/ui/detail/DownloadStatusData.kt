package net.xzos.upgradeall.ui.detail

import androidx.databinding.ObservableField
import com.tonyodev.fetch2.Status
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.application.MyApplication.Companion.context

class DownloadStatusData {
    val downloadStatusData = ObservableField<String>().apply {
        set(context.getString(R.string.download))
    }
    val downloadProgressData = ObservableField<Int>()

    fun setDownloadStatus(downloadStatus: Status) {
        downloadStatusData.set(context.getString(when (downloadStatus) {
            Status.NONE -> R.string.waiting
            Status.QUEUED -> R.string.queued
            Status.DOWNLOADING -> R.string.downloading
            Status.PAUSED -> R.string.paused
            Status.COMPLETED -> R.string.paused
            Status.CANCELLED -> R.string.paused
            Status.FAILED -> R.string.retry
            else -> R.string.download
        }))
    }

    fun getDownloadStatus(downloadStatus: Status): String {
        return MyApplication.context.getString(when (downloadStatus) {
            Status.NONE -> R.string.waiting
            Status.QUEUED -> R.string.queued
            Status.DOWNLOADING -> R.string.downloading
            Status.PAUSED -> R.string.paused
            Status.COMPLETED -> R.string.paused
            Status.CANCELLED -> R.string.paused
            Status.FAILED -> R.string.retry
            else -> R.string.download
        })
    }

    fun setDownloadProgress(downloadProgress: Int) {
        downloadProgressData.set(downloadProgress)
    }
}