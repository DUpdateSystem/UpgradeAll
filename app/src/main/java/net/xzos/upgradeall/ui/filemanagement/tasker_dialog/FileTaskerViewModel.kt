package net.xzos.upgradeall.ui.filemanagement.tasker_dialog

import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import com.tonyodev.fetch2.Download
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.utils.ObservableViewModel
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.wrapper.download.fileType
import net.xzos.upgradeall.wrapper.download.installFileTasker

class FileTaskerViewModel : ObservableViewModel() {
    private lateinit var fileTasker: FileTasker
    fun setFileTasker(fileTasker: FileTasker) {
        this.fileTasker = fileTasker
    }

    val installButtonVisibility = ObservableBoolean(false)
    val openFileButtonVisibility = ObservableBoolean(false)
    val pauseButtonVisibility = ObservableBoolean(false)
    val resumeButtonVisibility = ObservableBoolean(false)
    val retryButtonVisibility = ObservableBoolean(false)
    val deleteButtonVisibility = ObservableBoolean(false)

    val tagList: MutableLiveData<MutableList<String>> by lazy { mutableLiveDataOf() }
    val downloadList: MutableLiveData<MutableList<Download>> by lazy { mutableLiveDataOf() }

    fun onInstall(context: Context) {
        GlobalScope.launch {
            fileTasker.fileType(context)?.run {
                installFileTasker(context, fileTasker, this)
            }
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