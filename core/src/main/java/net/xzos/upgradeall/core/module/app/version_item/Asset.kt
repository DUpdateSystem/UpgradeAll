package net.xzos.upgradeall.core.module.app.version_item

import net.xzos.upgradeall.core.module.Hub

/**
 * 资源数据
 */
class Asset private constructor(
    /* 原始的版本号 */
    val versionNumber: String,
    /* 数据归属的软件源 */
    val hub: Hub,
    /* 更新日志 */
    val changelog: String?,
    /* 文件数据列表 */
    val fileAssetList: List<FileAsset>,
) {

    companion object {
        class TmpFileAsset(
            /* 文件数据名称，用来给用户看的 */
            val name: String,
            /* 默认下载链接 */
            internal val downloadUrl: String,
            internal val fileType: String?,
            internal val assetIndex: Pair<Int, Int>,
        )

        fun newInstance(
            versionNumber: String, hub: Hub, changelog: String?,
            _fileAssetList: List<TmpFileAsset>
        ): Asset {
            val fileAssetList: List<FileAsset> = _fileAssetList.map {
                FileAsset(it.name, it.downloadUrl, it.fileType, it.assetIndex, hub)
            }
            return Asset(versionNumber, hub, changelog, fileAssetList)
        }
    }
}