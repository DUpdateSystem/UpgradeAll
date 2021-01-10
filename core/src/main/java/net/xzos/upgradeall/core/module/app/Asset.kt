package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.module.Hub

class Asset(val hub: Hub, val changeLog: String, _app: App, _fileAssetList: List<FileAsset>) {
    private val fileAssetList: List<FileAsset> = _fileAssetList.map {
        FileAsset(it.name, it.downloadUrl, it.fileType, it.assetIndex, _app, hub)
    }
}