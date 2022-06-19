package net.xzos.upgradeall.core.utils.data_cache.cache_object

import net.xzos.upgradeall.core.utils.data_cache.CacheConfig

class AnyMemoryCache<T>(
    key: String,
    config: CacheConfig,
) : BaseCache<T>(key) {

    private var any: T? = null
    private val bytesDiskCache by lazy { config.dir?.let { BytesDiskCache(key, config) } }

    fun write(any: T?, encoder: Encoder<T>?) {
        any?.also { value ->
            this@AnyMemoryCache.any = value
            super.write(any)
            encoder?.run {
                bytesDiskCache?.write(this.encode(value))
            }
        }
    }

    override fun write(any: T?) {
        this.any = any
        super.write(any)
    }

    override fun delete() = true

    fun read(encoder: Encoder<T>?, throwError: Boolean = false): T? {
        val cache = any ?: encoder?.let {
            bytesDiskCache?.read()?.apply {
                any = it.decode(this)
            }
        }
        if (cache == null && throwError)
            throw NoCacheError()
        else return cache as T
    }

    override fun read(): T? = any
}