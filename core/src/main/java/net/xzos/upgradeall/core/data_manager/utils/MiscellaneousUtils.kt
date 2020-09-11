package net.xzos.upgradeall.core.data_manager.utils

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun <T> MutableList<T>.addItem(element: T, mutex: Mutex): Boolean {
    return runBlocking {
        mutex.withLock {
            this@addItem.add(element)
        }
    }
}

fun <T> MutableList<T>.removeItem(element: T, mutex: Mutex): Boolean {
    return runBlocking {
        mutex.withLock {
            this@removeItem.remove(element)
        }
    }
}
