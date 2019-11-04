package net.xzos.upgradeAll.server

import net.xzos.upgradeAll.server.app.manager.AppManager
import net.xzos.upgradeAll.server.log.LogUtil
import net.xzos.upgradeAll.json.gson.UIConfig
import org.jetbrains.annotations.Contract

internal class ServerContainer {
    companion object {
        @get:Contract(pure = true)
        val Log = LogUtil()
        @get:Contract(pure = true)
        val AppManager = AppManager()
    }
}
