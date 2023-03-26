package net.xzos.upgradeall.core.utils.coroutines

import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ValueMutex {
    private var value: Any? = NONE
    private val mutex = Mutex()

    fun <T> runWithLock(context: CoroutineContext = EmptyCoroutineContext, action: () -> T): T {
        return mutex.runWithLock(context) {
            if (value == NONE)
                action().apply { value = this }
            else
                @Suppress("UNCHECKED_CAST")
                value as T
        }
    }

    companion object {
        private val NONE = object {}
    }
}

class ValueMutexMap {
    private val map = hashMapOf<Any, ValueMutex>()
    private val mapMutex = Mutex()

    fun <T> runWith(
        key: Any,
        context: CoroutineContext = EmptyCoroutineContext,
        action: (ValueMutex) -> T
    ): T {
        val mutex = mapMutex.runWithLock(context) {
            map.getOrPut(key) { ValueMutex() }
        }
        return action(mutex).also {
            mapMutex.runWithLock(context) {
                map.remove(key)
            }
        }
    }

    fun <T> runWithLock(
        key: Any,
        context: CoroutineContext = EmptyCoroutineContext,
        action: (ValueMutex) -> T
    ): T {
        val mutex = mapMutex.runWithLock(context) {
            map.getOrPut(key) { ValueMutex() }
        }
        return mutex.runWithLock(context) {
            action(mutex)
        }.also {
            mapMutex.runWithLock(context) {
                map.remove(key)
            }
        }
    }
}