package net.xzos.upgradeall.core.utils.watchdog

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.utils.coroutines.lockWithCheck
import net.xzos.upgradeall.core.utils.coroutines.unlockWithCheck
import net.xzos.upgradeall.core.utils.coroutines.wait
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class WatchdogItem(private var timeoutMs: Long, private var pingMun: Int? = null) {
    private var pingTimeMs: Long = 0
    private val mutex = Mutex()

    private val listenerList = coroutinesMutableListOf<() -> Unit>(true)

    fun addStopListener(function: () -> Unit) {
        listenerList.add(function)
    }

    fun start(context: CoroutineContext = EmptyCoroutineContext) {
        GlobalScope.launch(context) {
            start()
        }
    }

    private suspend fun start() {
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

    internal fun renewStatus(): Boolean {
        return if (Date().time - pingTimeMs > timeoutMs) {
            stop()
            finish()
            false
        } else
            true
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