package net.xzos.upgradeall.ui.viewmodels.viewmodel

import net.xzos.upgradeall.core.downloader.DownloaderManager
import net.xzos.upgradeall.ui.viewmodels.view.FileItemView


class FileHubViewModel : ListContainerViewModel() {

    override fun loadData() {
        DownloaderManager.getDownloaderList().map { FileItemView(it.name, it) }
    }
}