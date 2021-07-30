package net.xzos.upgradeall.core.utils.watchdog

import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.utils.lockWithCheck
import net.xzos.upgradeall.core.utils.unlockWithCheck
import net.xzos.upgradeall.core.utils.wait
import java.util.*

class WatchdogItem(private var timeoutMs: Long, private var pingMun: Int? = null) {
    private var pingTimeMs: Long = 0
    private val mutex = Mutex()

    private val listenerList = coroutinesMutableListOf<() -> Unit>(true)

    fun addStopListener(function: () -> Unit) {
        listenerList.add(function)
    }

    suspend fun start() {
        mutex.lockWithCheck()
        pingTimeMs = Date().time
        WatchdogManager.addWatchdog(this)
    }

    fun stop() {
        WatchdogManager.removeWatchdog(this)
        mutex.unlockWithCheck()
    }

    fun ping() {
        if (pingMun != null) {
            if (pingMun!! <= 0) return
            if (pingMun!! > 0) pingMun = pingMun!! - 1
        }
        pingTimeMs = Date().time
    }

    internal fun renewStatus() {
        if (Date().time - pingTimeMs > timeoutMs) {
            stop()
            finish()
        }
    }

    fun resetTimeout(timeoutMs: Long) {
        this.timeoutMs = timeoutMs
    }

    suspend fun block() {
        mutex.wait()
    }

    private fun finish() {
        listenerList.forEach { it() }
    }
}