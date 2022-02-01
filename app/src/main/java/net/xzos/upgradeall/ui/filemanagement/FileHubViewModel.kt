package net.xzos.upgradeall.ui.filemanagement

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerManager
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.wrapper.download.fileTaskerManagerWrapper


class FileHubViewModel(application: Application) : ListContainerViewModel<FileItemView>(application) {

    override suspend fun doLoadData(): List<FileItemView> {
        return fileTaskerManagerWrapper.getFileTaskerList().map {
            FileItemView(it.name, it).apply {
                renewAppIcon(null, getApplication())
            }
        }
    }
}