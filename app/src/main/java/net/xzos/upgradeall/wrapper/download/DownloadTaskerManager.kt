package net.xzos.upgradeall.wrapper.download

import kotlinx.coroutines.launch
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.data.PreferencesMap

object DownloadTaskerManager {

    private val map = coroutinesMutableMapOf<String, DownloadTasker>(true)

    fun getFileTaskerList() = map.values

    fun register(downloadTasker: DownloadTasker) {
        addFileTasker(downloadTasker)
        downloadTasker.observe(
            checkerRunFun = { getFileTaskerList().contains(downloadTasker) },
            observerFun = {
                when (it.status()) {
                    DownloadTaskerStatus.INFO_RENEW -> {}
                    DownloadTaskerStatus.WAIT_START -> {}
                    DownloadTaskerStatus.START_FAIL -> {}
                    DownloadTaskerStatus.EXTERNAL_DOWNLOAD -> removeFileTasker(downloadTasker)
                    DownloadTaskerStatus.STARTED -> {}
                    DownloadTaskerStatus.DOWNLOAD_START -> {}
                    DownloadTaskerStatus.DOWNLOAD_RUNNING -> {}
                    DownloadTaskerStatus.DOWNLOAD_STOP -> {}
                    DownloadTaskerStatus.DOWNLOAD_COMPLETE -> {
                        if (PreferencesMap.auto_install)
                            MyApplication.applicationScope.launch { downloadTasker.install() }
                    }
                    DownloadTaskerStatus.DOWNLOAD_CANCEL -> removeFileTasker(downloadTasker)
                    DownloadTaskerStatus.DOWNLOAD_FAIL -> {}
                    DownloadTaskerStatus.NONE -> {}
                    DownloadTaskerStatus.INFO_COMPLETE -> {}
                    DownloadTaskerStatus.INFO_FAILED -> {}
                    DownloadTaskerStatus.IN_DOWNLOAD -> {}
                }
            })
    }

    private fun addFileTasker(downloadTasker: DownloadTasker) =
        map.put(downloadTasker.id, downloadTasker)

    fun removeFileTasker(downloadTasker: DownloadTasker) = map.remove(downloadTasker.id)

    fun getFileTasker(id: String): DownloadTasker? = map[id]
}