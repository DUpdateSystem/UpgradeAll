package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.module.Hub


/**
 * 文件数据列表
 * 用来提供下载{@link #download}、安装{@link #installable}{@link #install}功能
 */
class FileAsset(
        /* 文件数据名称，用来给用户看的 */
        val name: String,
        /* 默认下载链接 */
        internal val downloadUrl: String,
        internal val fileType: String,
        internal val assetIndex: Pair<Int, Int>,
        internal val app: App,
        internal val hub: Hub,
) {
    override fun equals(other: Any?): Boolean {
        return other is FileAsset
                && other.name == name
                && other.downloadUrl == downloadUrl
                && other.fileType == fileType
                && other.assetIndex == assetIndex
                && other.app == app
                && other.hub == hub
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + downloadUrl.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + assetIndex.hashCode()
        result = 31 * result + app.hashCode()
        result = 31 * result + hub.hashCode()
        return result
    }
}