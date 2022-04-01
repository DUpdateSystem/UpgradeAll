package net.xzos.upgradeall.utils

import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadStatusSnap

fun DownloadStatusSnap.progress() = downloadSize / totalSize * 100
