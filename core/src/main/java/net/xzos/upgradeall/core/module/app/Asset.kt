package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.module.Hub

/**
 * 资源数据
 */
class Asset(
        /* 数据归属的软件源 */
        val hub: Hub,
        /* 更新日志 */
        val changeLog: String?,
        _app: App, _fileAssetList: List<FileAsset>
) {
    /* 文件数据列表 */
    val fileAssetList: List<FileAsset> = _fileAssetList.map {
        FileAsset(it.name, it.downloadUrl, it.fileType, it.assetIndex, _app, hub)
    }
}