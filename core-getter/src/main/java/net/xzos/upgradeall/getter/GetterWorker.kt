package net.xzos.upgradeall.getter

import kotlinx.coroutines.delay

lateinit var GETTER_PORT: GetterPort

suspend fun runGetterServer(getterPort: GetterPort) {
    GETTER_PORT = getterPort
    GETTER_PORT.runService()
}

suspend fun waitGetterServer() {
    while (!GETTER_PORT.waitService()) {
        delay(1000L)
    }
}