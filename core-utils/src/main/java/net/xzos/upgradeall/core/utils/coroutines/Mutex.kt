package net.xzos.upgradeall.core.utils.coroutines

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun Mutex.unlockAfterComplete(action: () -> Unit) {
    action()
    unlockWithCheck()
}

fun Mutex.lockWithCheck(owner: Any? = null) {
    if (!this.isLocked)
        runBlocking { this@lockWithCheck.lock(owner) }
}

fun Mutex.unlockWithCheck() {
    try {
        this.unlock()
    } catch (ignore: IllegalStateException) {
    }
}

suspend fun Mutex.wait() {
    if (this.isLocked) {
        try {
            withLock { }
        } catch (ignore: IllegalStateException) {
        }
    }
}

fun <T> Mutex.runWithLock(context: CoroutineContext = EmptyCoroutineContext, action: () -> T): T {
    return runBlocking(context) {
        this@runWithLock.withLock {
            action()
        }
    }
}