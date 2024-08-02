package net.xzos.upgradeall.getter

import android.content.Context
import kotlinx.coroutines.delay

lateinit var GETTER_PORT: GetterPort

suspend fun runGetterServer(context: Context, getterPort: GetterPort) {
    GETTER_PORT = getterPort
    GETTER_PORT.runService(context)
}

suspend fun waitGetterServer() {
    while (!GETTER_PORT.waitService()) {
        delay(1000L)
    }
}