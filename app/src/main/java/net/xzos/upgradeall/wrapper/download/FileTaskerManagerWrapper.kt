package net.xzos.upgradeall.wrapper.download

import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerId
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerManager

class FileTaskerManagerWrapper(private val manager: FileTaskerManager) {

    fun getFileTaskerList() = manager.getFileTaskerList() as List<FileTaskerWrapper>

    fun getFileTasker(
        id: FileTaskerId? = null, owner: Any? = null, idString: String? = null
    ) = manager.getFileTasker(id, owner, idString) as FileTaskerWrapper?
}

val fileTaskerManagerWrapper = FileTaskerManagerWrapper(FileTaskerManager)
