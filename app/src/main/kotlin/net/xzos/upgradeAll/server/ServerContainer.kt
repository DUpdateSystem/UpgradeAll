package net.xzos.upgradeAll.server

import net.xzos.upgradeAll.server.log.LogUtil
import net.xzos.upgradeAll.server.app.manager.AppManager

import org.jetbrains.annotations.Contract

class ServerContainer {
    object AppServer {
        @get:Contract(pure = true)
        val log = LogUtil()
        @get:Contract(pure = true)
        val appManager = AppManager()
    }
}
