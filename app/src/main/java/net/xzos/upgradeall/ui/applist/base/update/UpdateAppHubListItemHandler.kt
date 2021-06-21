package net.xzos.upgradeall.ui.applist.base.update

import android.view.View
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.downloader.DownloadOb
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.server.downloader.startDownload
import net.xzos.upgradeall.ui.applist.base.AppHubListItemHandler

class UpdateAppHubListItemHandler : AppHubListItemHandler() {
    fun clickDownload(app: App, view: View) {
        val fileAsset = app.versionList.firstOrNull()
            ?.assetList?.firstOrNull()
            ?.fileAssetList?.firstOrNull()
            ?: return
        GlobalScope.launch {
            startDownload(
                app.appId, fileAsset,
                {}, {}, DownloadOb.getEmptyDownloadOb(),
                view.context, false
            )
        }
    }
}