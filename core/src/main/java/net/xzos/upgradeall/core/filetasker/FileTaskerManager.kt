package net.xzos.upgradeall.core.filetasker

import net.xzos.upgradeall.core.downloader.renewDownloadServiceStatus
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf

object FileTaskerManager {
    private val fileTaskerList = coroutinesMutableListOf<FileTasker>(true)

    fun getFileTaskerList(): List<FileTasker> = fileTaskerList

    fun getFileTasker(fileTaskerId: Int): FileTasker? {
        val list = fileTaskerList.filter { it.id == fileTaskerId }
        return if (list.isNotEmpty())
            list[0]
        else null
    }

    fun getFileTasker(fileAsset: FileAsset): FileTasker? {
        val list = fileTaskerList.filter { it.fileAsset == fileAsset }
        return if (list.isNotEmpty())
            list[0]
        else null
    }

    internal fun addFileTasker(fileTasker: FileTasker) {
        fileTaskerList.add(fileTasker)
    }

    internal fun removeFileTasker(fileTasker: FileTasker) {
        fileTasker.downloader?.removeFile()
        fileTaskerList.remove(fileTasker)
        renewDownloadServiceStatus()
    }
}