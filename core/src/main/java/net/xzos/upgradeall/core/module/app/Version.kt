package net.xzos.upgradeall.core.module.app

/**
 * 版本号数据
 */
class Version(
        /* 版本号 */
        val name: String,
        /* 资源列表 */
        val assetList: MutableList<Asset>
) {
    /* 忽略这个版本 */
    fun ignore() {}
    /* 取消忽略这个版本 */
    fun unignore() {}
}