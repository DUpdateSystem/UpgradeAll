package net.xzos.upgradeall.core.downloader.filedownloader.item

import net.xzos.upgradeall.core.downloader.filedownloader.getNewRandomNameFile
import java.io.File

class DownloadFile(private val parent: File) {
    val file: File by lazy {
        getNewRandomNameFile(parent, true)
    }

    fun delete() {
        file.delete()
    }
}