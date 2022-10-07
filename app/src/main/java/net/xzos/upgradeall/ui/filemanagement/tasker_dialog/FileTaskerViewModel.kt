package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.app.Application
import android.content.Context
import androidx.databinding.ObservableBoolean
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.downloader.filedownloader.item.TaskWrapper
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.utils.ObservableViewModel
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.utils.setList
import net.xzos.upgradeall.wrapper.download.*

class FileTaskerViewModel : ObservableViewModel() {
    private lateinit var fileTasker: DownloadTasker
    fun setFileTasker(fileTasker: DownloadTasker) {
        this.fileTasker = fileTasker
        this.fileTasker.observe(observerFun)
    }

    val installButtonVisibility = ObservableBoolean(false)
    val openFileButtonVisibility = ObservableBoolean(false)
    val pauseButtonVisibility = ObservableBoolean(false)
    val resumeButtonVisibility = ObservableBoolean(false)
    val retryButtonVisibility = ObservableBoolean(false)
    val deleteButtonVisibility = ObservableBoolean(true)

    val tagList by lazy {
        mutableLiveDataOf<MutableList<String>>().apply {
            value = mutableListOf()
        }
    }
    val downloadList by lazy {
        mutableLiveDataOf<MutableList<TaskWrapper>>().apply {
            value = mutableListOf()
        }
    }

    private fun setStatus(status: DownloadTaskerStatus) {
        when (status) {
            DownloadTaskerStatus.DOWNLOAD_START -> {
                flashActive()
                pauseButtonVisibility.set(true)
            }
            DownloadTaskerStatus.DOWNLOAD_RUNNING -> {
                flashActive()
                pauseButtonVisibility.set(true)
            }
            DownloadTaskerStatus.DOWNLOAD_STOP -> {
                flashActive()
                resumeButtonVisibility.set(true)
            }
            DownloadTaskerStatus.DOWNLOAD_COMPLETE -> {
                flashActive()
                openFileButtonVisibility.set(true)
                checkInstall()
            }
            DownloadTaskerStatus.DOWNLOAD_CANCEL -> {
                downloadList.setList(emptyList())
            }
            DownloadTaskerStatus.DOWNLOAD_FAIL -> {
                flashActive()
                retryButtonVisibility.set(true)
            }
            else -> {
                flashActive()
            }
        }
    }

    fun renew() {
        setStatus(fileTasker.snap.status())
    }

    private val observerFun = fun(snap: DownloadTaskerSnap) {
        setStatus(snap.status())
    }

    private fun flashActive() {
        resetView()
        downloadList.setList(runBlocking { fileTasker.downloader?.getTaskList() ?: listOf() })
    }

    private fun resetView() {
        installButtonVisibility.set(false)
        openFileButtonVisibility.set(false)
        pauseButtonVisibility.set(true)
        resumeButtonVisibility.set(false)
        retryButtonVisibility.set(false)
        deleteButtonVisibility.set(true)

        renewTag()
    }

    private fun renewTag() {
        tagList.setList(listOf(fileTasker.fileType.desc))
    }

    private fun checkInstall() {
        if (fileTasker.fileType != FileType.UNKNOWN) {
            installButtonVisibility.set(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        fileTasker.removeObserver(observerFun)
    }

    fun onInstall(context: Context) {
        GlobalScope.launch {
            fileTasker.install(context)
        }
    }

    fun onOpen() {
    }

    fun onPause() {
        fileTasker.downloader?.pause()
    }

    fun onResume() {
        fileTasker.downloader?.resume()
    }

    fun onRetry() {
        fileTasker.downloader?.retry()
    }

    fun onDelete() {
        fileTasker.downloader?.cancel()
    }
}

class FileTaskerListViewModel(
    application: Application
) : ListContainerViewModel<TaskWrapper>(application) {
    lateinit var getDownload: () -> List<TaskWrapper>
    override suspend fun doLoadData(): List<TaskWrapper> = getDownload()
}