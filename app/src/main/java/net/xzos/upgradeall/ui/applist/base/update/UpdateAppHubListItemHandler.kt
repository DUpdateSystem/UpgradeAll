package net.xzos.upgradeall.ui.applist.base.update

import android.view.View
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.applist.base.AppHubListItemHandler
import net.xzos.upgradeall.wrapper.download.startDownload

class UpdateAppHubListItemHandler : AppHubListItemHandler() {
    fun clickDownload(app: App, view: View) {
        val fileAsset = app.versionList.firstOrNull()
            ?.assetList?.firstOrNull()
            ?.fileAssetList?.firstOrNull()
            ?: return
        GlobalScope.launch {
            startDownload(
                view.context, false,
                app, fileAsset,
            )
        }
    }
}