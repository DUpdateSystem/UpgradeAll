package net.xzos.upgradeall.ui.viewmodels.viewmodel

import android.app.Application
import net.xzos.upgradeall.core.downloader.DownloaderManager
import net.xzos.upgradeall.ui.viewmodels.view.FileItemView


class FileHubViewModel(application: Application) : ListContainerViewModel<FileItemView>(application) {

    override suspend fun doLoadData(): List<FileItemView> {
        return DownloaderManager.getDownloaderList().map {
            FileItemView(it.name, it)
        }
    }
}