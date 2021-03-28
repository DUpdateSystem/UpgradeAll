package net.xzos.upgradeall.ui.filemanagement

import android.app.Application
import net.xzos.upgradeall.core.filetasker.FileTaskerManager
import net.xzos.upgradeall.ui.base.recycleview.ListContainerViewModel


class FileHubViewModel(application: Application) : ListContainerViewModel<FileItemView>(application) {

    override suspend fun doLoadData(): List<FileItemView> {
        return FileTaskerManager.getFileTaskerList().map {
            FileItemView(it.name, it).apply {
                renewAppIcon(null, getApplication())
            }
        }
    }
}