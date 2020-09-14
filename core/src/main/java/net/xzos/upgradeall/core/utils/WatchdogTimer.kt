package net.xzos.upgradeall.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WatchdogTimer(
        private val timeoutSecond: Int,
        private val function: () -> Unit
) {
    private var counter = timeoutSecond

    init {
        GlobalScope.launch(Dispatchers.Unconfined) {
            while (counter > 0) {
                delay(timeoutSecond * 1000L)
                counter -= timeoutSecond
            }
            function()
        }
    }

    fun reset() {
        counter = timeoutSecond
    }
}