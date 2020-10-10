package net.xzos.upgradeall.core.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WatchdogTimer(
        private val timeoutSecond: Int,
        private val function: () -> Unit
) {
    private var counter = timeoutSecond
    private val mutex = Mutex()

    init {
        GlobalScope.launch {
            while (counter > 0) {
                delay(timeoutSecond * 1000L)
                mutex.withLock {
                    counter -= timeoutSecond
                }
            }
            function()
        }
    }

    fun reset() {
        mutex.runWithLock {
            counter = timeoutSecond
        }
    }
}