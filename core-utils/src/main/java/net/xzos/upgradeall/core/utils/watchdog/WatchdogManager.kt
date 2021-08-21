package net.xzos.upgradeall.core.utils.watchdog

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf

internal object WatchdogManager {

    private val renewMutex = Mutex()

    var delay: Long = 5000
    private val watchdogItemList = coroutinesMutableListOf<WatchdogItem>(true)

    suspend fun addWatchdog(watchdog: WatchdogItem) {
        if (watchdogItemList.add(watchdog))
            runRenew()
    }

    fun removeWatchdog(watchdog: WatchdogItem) {
        watchdogItemList.remove(watchdog)
    }

    private fun renewWatchdog() {
        watchdogItemList.forEach { it.renewStatus() }
    }

    private suspend fun runRenew() {
        if (!renewMutex.isLocked)
            renewMutex.withLock {
                while (watchdogItemList.isNotEmpty()) {
                    delay(delay)
                    renewWatchdog()
                }
            }
    }
}