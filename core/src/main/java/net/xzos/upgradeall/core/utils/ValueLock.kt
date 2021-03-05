package net.xzos.upgradeall.core.utils

import kotlinx.coroutines.sync.Mutex

class ValueLock<T> {
    private val mutex = Mutex(true)

    private var _value: T? = null

    fun isEmpty() = mutex.isLocked

    fun refresh() {
        mutex.lockWithCheck()
        _value = null
    }

    fun setValue(value: T?) {
        mutex.unlockAfterComplete {
            _value = value
        }
    }

    suspend fun getValue(block: Boolean = true): T? {
        if (block) mutex.wait()
        return _value
    }
}