package net.xzos.upgradeall.core.utils

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex

class ValueLock<T> {
    private val mutex = Mutex(true)

    private var _value: T? = null

    var value: T
        get() {
            return runBlocking {
                mutex.wait()
                _value!!
            }
        }
        set(value) {
            runBlocking {
                mutex.unlockAfterComplete {
                    _value = value
                }
            }
        }
}