package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.app.Application
import android.content.Context
import androidx.databinding.ObservableBoolean
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadStatus
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadStatusSnap
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
        this.fileTasker.observeForever(observerFun)
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
        mutableLiveDataOf<MutableList<DownloadStatusSnap>>().apply {
            value = mutableListOf()
        }
    }

    private fun setStatus(snap: DownloadTaskerSnap) {
        when (snap.getDownloadStatus().first()) {
            DownloadStatus.START -> {
                flashActive()
                pauseButtonVisibility.set(true)
            }
            DownloadStatus.RUNNING -> {
                flashActive()
                pauseButtonVisibility.set(true)
            }
            DownloadStatus.STOP -> {
                flashActive()
                resumeButtonVisibility.set(true)
            }
            DownloadStatus.COMPLETE -> {
                flashActive()
                openFileButtonVisibility.set(true)
                checkInstall()
            }
            DownloadStatus.CANCEL -> {
                downloadList.setList(emptyList())
            }
            DownloadStatus.FAIL -> {
                flashActive()
                retryButtonVisibility.set(true)
            }
            else -> {
                flashActive()
            }
        }
    }

    fun renew() {
        setStatus(fileTasker.snap)
    }

    private val observerFun = fun(snap: DownloadTaskerSnap) {
        setStatus(snap)
    }

    private fun flashActive() {
        resetView()
        downloadList.setList(runBlocking { fileTasker.getDownloadList() })
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
            installFileTasker(context, fileTasker)
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
) : ListContainerViewModel<DownloadTaskerSnap>(application) {
    lateinit var getDownload: () -> List<DownloadTaskerSnap>
    override suspend fun doLoadData(): List<DownloadTaskerSnap> = getDownload()
}