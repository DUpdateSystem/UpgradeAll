package net.xzos.upgradeall.ui.viewmodels.viewmodel

import android.app.Application
import net.xzos.upgradeall.ui.viewmodels.view.FileItemView


class FileHubViewModel(application: Application) : ListContainerViewModel<FileItemView>(application) {

    override fun doLoadData(): List<FileItemView> {
        return emptyList()
    }
}