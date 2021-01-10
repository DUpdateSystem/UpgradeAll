package net.xzos.upgradeall.core.module.app

class Version(val name: String, val assetList: MutableList<Asset>) {
    fun ignore() {}
    fun unignore() {}
}