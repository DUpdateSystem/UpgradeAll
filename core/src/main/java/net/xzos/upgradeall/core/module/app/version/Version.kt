package net.xzos.upgradeall.core.module.app.version

import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.websdk.data.json.AssetGson
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

/**
 * 版本号数据的快照
 */
data class Version(
    val versionInfo: VersionInfo,
    val versionList: List<VersionWrapper>,
) {
    fun getAssetList() = versionList.flatMap { it.assetList }
}

data class VersionWrapper(
    val hub: Hub,
    val release: ReleaseGson,
    val assetList: List<AssetWrapper>,
)

data class AssetWrapper(
    val hub: Hub,
    val assetIndex: List<Int>,
    val asset: AssetGson,
)
