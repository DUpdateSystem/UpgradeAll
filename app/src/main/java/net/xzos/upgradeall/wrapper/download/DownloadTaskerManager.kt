package net.xzos.upgradeall.wrapper.download

import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf

object DownloadTaskerManager {

    private val map = coroutinesMutableMapOf<Any, DownloadTasker>()

    fun getFileTaskerList() = map.values

    fun addFileTasker(
        owner: Any, downloadTasker: DownloadTasker
    ) = if (getFileTasker(owner) != null)
        map.put(owner, downloadTasker).let { true }
    else false

    fun getFileTasker(id: String): DownloadTasker? {
        getFileTaskerList().forEach {
            if (it.toString() == id)
                return it
        }
        return null
    }

    fun getFileTasker(owner: Any): DownloadTasker? {
        return map.getOrDefault(owner, null)
    }

    fun removeFileTasker(owner: Any) = map.remove(owner)

    fun removeFileTasker(downloadTasker: DownloadTasker) {
        downloadTasker.downloader?.removeFile()
        var owner: Any? = null
        for (item in map.iterator()) {
            if (item.value == downloadTasker) {
                owner = item.key
                break
            }
        }
        owner?.let {
            removeFileTasker(owner)
        }
    }
}