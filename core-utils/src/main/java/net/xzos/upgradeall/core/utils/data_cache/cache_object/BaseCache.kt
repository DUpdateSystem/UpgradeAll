package net.xzos.upgradeall.core.utils.data_cache.cache_object

import java.time.Instant

abstract class BaseCache<T>(
    val key: String
) {
    var time: Long = 0L

    init {
        renewTime()
    }

    fun checkValid(dataCacheTimeSec: Int): Boolean {
        return (Instant.now().epochSecond - time <= dataCacheTimeSec)
    }

    private fun renewTime() {
        time = System.currentTimeMillis()
    }

    open fun write(any: T?) {
        any?.run { renewTime() }
    }

    abstract fun read(): T?

    abstract fun delete(): Boolean
}