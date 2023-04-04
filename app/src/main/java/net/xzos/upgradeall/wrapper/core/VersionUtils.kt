package net.xzos.upgradeall.wrapper.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionEntityUtils
import net.xzos.upgradeall.core.module.app.version.VersionInfo

fun VersionInfo.switchIgnoreStatus(app: App) = runBlocking(Dispatchers.Default) {
    VersionEntityUtils(app).switchIgnoreStatus(name)
}

fun VersionInfo.isIgnored(app: App) = runBlocking(Dispatchers.Default) {
    VersionEntityUtils(app).isIgnored(name)
}
