package net.xzos.upgradeAll.server

import net.xzos.upgradeAll.server.log.LogUtil
import net.xzos.upgradeAll.server.update.UpdateManager
import org.jetbrains.annotations.Contract

internal class ServerContainer {
    companion object {
        @get:Contract(pure = true)
        val Log = LogUtil()
    }
}
