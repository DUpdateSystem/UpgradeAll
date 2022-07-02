package net.xzos.upgradeall.wrapper.core

import android.content.Context
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.wrapper.download.startDownload

suspend fun App.upgrade(context: Context) {
    val fileAsset = versionList.firstOrNull()
        ?.versionList?.firstOrNull()
        ?.assetList?.firstOrNull()
        ?: return
    startDownload(context, false, this, fileAsset)
}
