package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.content.Context
import androidx.databinding.ObservableBoolean
import com.tonyodev.fetch2.Download
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerSnap
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerStatus
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.utils.ObservableViewModel
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.utils.setList
import net.xzos.upgradeall.wrapper.download.FileTaskerWrapper
import net.xzos.upgradeall.wrapper.download.installFileTasker

class FileTaskerViewModel : ObservableViewModel() {
    private lateinit var fileTasker: FileTaskerWrapper
    fun setFileTasker(fileTasker: FileTaskerWrapper) {
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
        mutableLiveDataOf<MutableList<Download>>().apply {
            value = mutableListOf()
        }
    }

    private fun setStatus(snap: FileTaskerSnap) {
        when (snap.status) {
            FileTaskerStatus.DOWNLOAD_START -> {
                flashActive()
                pauseButtonVisibility.set(true)
            }
            FileTaskerStatus.DOWNLOAD_RUNNING -> {
                flashActive()
                pauseButtonVisibility.set(true)
            }
            FileTaskerStatus.DOWNLOAD_STOP -> {
                flashActive()
                resumeButtonVisibility.set(true)
            }
            FileTaskerStatus.DOWNLOAD_COMPLETE -> {
                flashActive()
                openFileButtonVisibility.set(true)
                checkInstall()
            }
            FileTaskerStatus.DOWNLOAD_CANCEL -> {
                downloadList.setList(emptyList())
            }
            FileTaskerStatus.DOWNLOAD_FAIL -> {
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

    private val observerFun = fun(snap: FileTaskerSnap) {
        setStatus(snap)
    }

    private fun flashActive() {
        resetView()
        GlobalScope.launch {
            downloadList.setList(fileTasker.getDownloadList())
        }
        // Fetch 获取 Download 列表会导致 UI 阻塞
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
        fileTasker.pause()
    }

    fun onResume() {
        fileTasker.resume()
    }

    fun onRetry() {
        fileTasker.retry()
    }

    fun onDelete() {
        fileTasker.cancel()
    }
}