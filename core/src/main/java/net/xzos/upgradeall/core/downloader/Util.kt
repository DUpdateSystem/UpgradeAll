package net.xzos.upgradeall.core.downloader

import net.xzos.upgradeall.core.filetasker.FileTaskerManager

fun renewDownloadServiceStatus() {
    if (DownloaderManager.getDownloaderList().isEmpty()
            || FileTaskerManager.getFileTaskerList().isEmpty()) {
        DownloadService.close()
    }
}