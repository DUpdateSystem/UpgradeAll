package net.xzos.upgradeall.wrapper.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version.Version

fun Version.switchIgnoreStatus(app: App) = runBlocking(Dispatchers.Default) {
    app.entityUtils.switchIgnoreStatus(versionInfo.name)
}

fun Version.isIgnored(app: App) = runBlocking(Dispatchers.Default) {
    app.entityUtils.isIgnored(versionInfo.name)
}
