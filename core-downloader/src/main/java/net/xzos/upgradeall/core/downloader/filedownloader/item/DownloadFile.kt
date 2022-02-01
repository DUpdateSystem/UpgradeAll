package net.xzos.upgradeall.core.downloader.filedownloader.item

import net.xzos.upgradeall.core.downloader.filedownloader.getNewRandomNameFile
import java.io.File

class DownloadFile(private val parent: File) {
    private val dirFile: File by lazy {
        getNewRandomNameFile(parent, true)
    }

    fun getFile(name:String) = File(dirFile, name)

    fun getFileList(): List<File>? = dirFile.listFiles()?.toList()

    fun delete() {
        dirFile.delete()
    }
}