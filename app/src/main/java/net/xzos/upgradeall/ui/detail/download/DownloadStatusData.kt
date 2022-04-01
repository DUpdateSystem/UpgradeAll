package net.xzos.upgradeall.ui.detail.download

import androidx.databinding.ObservableField
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication.Companion.context

class DownloadStatusData {
    val downloadStatusData = ObservableField<String>().apply {
        set(context.getString(R.string.download))
    }
    val downloadProgressData = ObservableField<Int>()

    fun setDownloadProgress(downloadProgress: Int) {
        downloadProgressData.set(downloadProgress)
    }
}