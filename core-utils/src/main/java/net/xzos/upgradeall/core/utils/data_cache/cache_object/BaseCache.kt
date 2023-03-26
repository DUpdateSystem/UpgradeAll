package net.xzos.upgradeall.core.utils.data_cache.cache_object

import java.time.Instant

abstract class BaseCache<B>(
    val key: String
) {
    abstract val store: BaseStore<B>

    fun checkValid(dataCacheTimeSec: Int): Boolean {
        return (Instant.now().epochSecond - store.getTime() <= dataCacheTimeSec)
    }

    private fun renewTime() {
        store.setTime(Instant.now().epochSecond)
    }

    fun <T> write(any: T?, encoder: Encoder<T, B>) {
        store.write(encoder.encode(any))
        renewTime()
    }

    fun <T> read(encoder: Encoder<T, B>): T? {
        return encoder.decode(store.read())
    }

    fun delete() {
        store.delete()
    }
}

interface BaseStore<T> {
    fun setTime(time: Long)
    fun getTime(): Long
    fun write(data: T)
    fun read(): T
    fun delete()
}