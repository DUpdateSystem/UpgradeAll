package net.xzos.upgradeall.core.downloader.filetasker

import net.xzos.upgradeall.core.downloader.filedownloader.renewDownloadServiceStatus
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf

object FileTaskerManager {
    private val fileTaskerList = coroutinesMutableListOf<FileTasker>(true)

    fun getFileTaskerList(): List<FileTasker> = fileTaskerList

    fun getFileTasker(
        id: FileTaskerId? = null, owner: Any? = null, idString: String? = null
    ): FileTasker? {
        fileTaskerList.forEach {
            if (it.id.toString() == idString || it.id == id || it.id.owner == owner)
                return it
        }
        return null
    }

    internal fun addFileTasker(fileTasker: FileTasker): Boolean {
        return if (getFileTasker(fileTasker.id, fileTasker.id.owner) == null)
            fileTaskerList.add(fileTasker)
        else false
    }

    internal fun removeFileTasker(fileTasker: FileTasker) {
        fileTasker.downloader.removeFile()
        fileTaskerList.remove(fileTasker)
        renewDownloadServiceStatus()
    }
}