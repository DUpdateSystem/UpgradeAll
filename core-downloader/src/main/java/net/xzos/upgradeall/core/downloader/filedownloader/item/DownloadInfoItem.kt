package net.xzos.upgradeall.core.downloader.filedownloader.item

class DownloadInfoItem(
    val name: String,
    val url: String,
    val headers: Map<String, String>,
    val cookies: Map<String, String>,
)