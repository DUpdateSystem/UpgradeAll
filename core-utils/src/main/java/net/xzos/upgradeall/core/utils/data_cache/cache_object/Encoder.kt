package net.xzos.upgradeall.core.utils.data_cache.cache_object

interface Encoder<I, O> {
    fun encode(obj: I?): O
    fun decode(data: O): I?
}

interface BytesEncoder<T> : Encoder<T, ByteArray>

class PassEncoder<T> : Encoder<T, T> {
    override fun encode(obj: T?) = obj!!
    override fun decode(data: T) = data
}