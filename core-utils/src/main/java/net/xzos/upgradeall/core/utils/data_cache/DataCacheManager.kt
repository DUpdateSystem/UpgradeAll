package net.xzos.upgradeall.core.utils.data_cache

import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.ValueMutex
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.data_cache.cache_object.*


class DataCacheManager(var config: CacheConfig) {

    fun <T> get(
        mutex: ValueMutex, saveMode: SaveMode,
        key: String, encoder: BytesEncoder<T>?,
        renewFun: () -> T? = { null },
    ): T? {
        return get(saveMode, key, encoder) { null }
            ?: mutex.runWithLock { get(saveMode, key, encoder, renewFun) }
    }

    fun <T> get(
        saveMode: SaveMode, key: String, encoder: BytesEncoder<T>?, renewFun: () -> T? = { null }
    ): T? {
        try {
            when (saveMode) {
                SaveMode.MEMORY_ONLY -> anyCacheMap[key]?.force<T>()?.readCheck()
                SaveMode.DISK_ONLY -> BytesDiskCache(key, config).readCheck(encoder!!)
                SaveMode.MEMORY_AND_DISK -> {
                    anyCacheMap.getOrPut(key) {
                        AnyMemoryCache<T>(key).apply {
                            BytesDiskCache(key, config).readCheck(encoder!!)?.run { write(this) }
                        }
                    }.force<T>().readCheck()
                }
            }?.let {
                return it
            }
        } catch (_: Throwable) {
        }
        return renewFun()?.also {
            set(saveMode, key, encoder!!, it)
        }
    }

    fun del(
        saveMode: SaveMode, key: String
    ) {
        try {
            when (saveMode) {
                SaveMode.MEMORY_ONLY -> anyCacheMap[key]?.delete()
                SaveMode.DISK_ONLY -> BytesDiskCache(key, config).delete()
                SaveMode.MEMORY_AND_DISK -> {
                    anyCacheMap.remove(key)
                    BytesDiskCache(key, config).delete()
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun <T> BytesDiskCache.readCheck(encoder: Encoder<T, ByteArray>): T? {
        return if (checkValid(config.defExpires)) {
            read(encoder)
        } else {
            delete()
            null
        }
    }

    private fun <T> AnyMemoryCache<T>.readCheck(): T? {
        return if (checkValid(config.defExpires)) {
            read()
        } else {
            delete()
            null
        }
    }

    fun <T> set(saveMode: SaveMode, key: String, encoder: BytesEncoder<T>, value: T?) {
        when (saveMode) {
            SaveMode.MEMORY_ONLY ->
                anyCacheMap.getOrPut(key) { AnyMemoryCache<T>(key) }.force<T>().write(value)
            SaveMode.DISK_ONLY -> BytesDiskCache(key, config).write(value, encoder)
            SaveMode.MEMORY_AND_DISK -> {
                anyCacheMap.getOrPut(key) { AnyMemoryCache<T>(key) }.force<T>().write(value)
                BytesDiskCache(key, config).write(value, encoder)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> AnyMemoryCache<*>.force(): AnyMemoryCache<T> = this as AnyMemoryCache<T>

    companion object {
        private val anyCacheMap: CoroutinesMutableMap<
                String, AnyMemoryCache<*>> = coroutinesMutableMapOf(true)
    }
}