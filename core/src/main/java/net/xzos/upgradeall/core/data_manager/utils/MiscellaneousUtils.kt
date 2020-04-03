package net.xzos.upgradeall.core.data_manager.utils

import kotlinx.coroutines.sync.Mutex

suspend fun Mutex.wait() {
    if (this.isLocked) {
        this.lock()
        this.unlock()
    }
}
