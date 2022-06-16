package net.xzos.upgradeall.core.utils.data_cache.cache_object

interface Encoder<T> {
    fun encode(value: T?): ByteArray
    fun decode(bytes: ByteArray): T?
}