package net.xzos.upgradeall.core.module.app.version_item

import net.xzos.upgradeall.core.module.Hub


/**
 * 文件数据列表
 * 用来提供下载{@link #download}、安装{@link #installable}{@link #install}功能
 */
data class FileAsset(
    /* 文件数据名称，用来给用户看的 */
    val name: String,
    /* 默认下载链接 */
    val downloadUrl: String?,
    val fileType: String?,
    val assetIndex: Pair<Int, Int>,
    val hub: Hub,
)