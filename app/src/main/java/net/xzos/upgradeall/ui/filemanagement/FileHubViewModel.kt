package net.xzos.upgradeall.ui.filemanagement

import android.app.Application
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel
import net.xzos.upgradeall.wrapper.download.DownloadTaskerManager
import net.xzos.upgradeall.wrapper.download.defName


class FileHubViewModel(application: Application) :
    ListContainerViewModel<FileItemView>(application) {

    override suspend fun doLoadData(): List<FileItemView> {
        return DownloadTaskerManager.getFileTaskerList().map {
            FileItemView(it.defName(), it).apply {
                renewAppIcon(null, getApplication())
            }
        }
    }
}