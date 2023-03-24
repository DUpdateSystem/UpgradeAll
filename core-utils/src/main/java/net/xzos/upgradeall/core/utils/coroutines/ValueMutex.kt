package net.xzos.upgradeall.core.utils.coroutines

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ValueMutex {
    private var value: Any? = NONE
    private val mutex = Mutex()

    fun <T> runWithLock(context: CoroutineContext = EmptyCoroutineContext, action: () -> T): T {
        return runBlocking(context) {
            mutex.withLock {
                if (value == NONE)
                    action().apply { value = this }
                else
                    @Suppress("UNCHECKED_CAST")
                    value as T
            }
        }
    }

    companion object {
        private val NONE = object {}
    }
}