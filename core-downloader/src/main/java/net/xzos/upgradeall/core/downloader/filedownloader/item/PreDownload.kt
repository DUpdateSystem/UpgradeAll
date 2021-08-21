package net.xzos.upgradeall.core.downloader.filedownloader.item

import java.io.File

internal class PreDownload {
    fun setDownload(downloadFile: File, downloadInfoList: List<DownloadInfoItem>): Downloader {
        return Downloader(downloadFile).apply {
            downloadInfoList.forEach {
                addTask(it.name, it.url, it.headers, it.cookies)
            }
        }
    }
}