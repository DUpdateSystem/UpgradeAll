package net.xzos.upgradeall.core.data.coroutines_basic_data_type

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


suspend fun Mutex.wait() {
    if (this.isLocked) {
        this.lock()
        this.unlock()
    }
}

fun <T> Mutex.runWithLock(action: () -> T): T {
    return runBlocking {
        this@runWithLock.withLock {
            action()
        }
    }
}
