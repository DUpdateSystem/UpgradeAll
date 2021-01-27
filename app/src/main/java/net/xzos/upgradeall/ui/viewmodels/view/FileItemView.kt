package net.xzos.upgradeall.ui.viewmodels.view

import net.xzos.upgradeall.core.downloader.Downloader

class FileItemView(
        val name: String,
        val downloader: Downloader,
) : ListView()