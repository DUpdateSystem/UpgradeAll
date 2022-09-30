package net.xzos.upgradeall.core.utils.data_cache

import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.coroutines.runWithLock
import net.xzos.upgradeall.core.utils.data_cache.cache_object.AnyMemoryCache
import net.xzos.upgradeall.core.utils.data_cache.cache_object.Encoder
import net.xzos.upgradeall.core.utils.data_cache.cache_object.NoCacheError
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
import net.xzos.upgradeall.core.utils.md5


class DataCacheManager(var config: CacheConfig) {

    private val cacheContainer = Cache(config)

    fun <T> getAll(): Map<String, T?> {
        return cacheContainer.getKeys().mapNotNull {
            val cache = cacheContainer.getCache<T>(it) ?: return@mapNotNull null
            it to cache.read()
        }.toMap()
    }

    fun <E> get(
        mutex: Mutex,
        key: String, saveMode: SaveMode, encoder: Encoder<E>?,
        nullable: Boolean = false, renewFun: () -> E? = { null },
    ): E? {
        val value = get(key, saveMode, encoder, nullable) { null }
        return if (value != null || nullable) value
        else mutex.runWithLock {
            get(key, saveMode, encoder, nullable, renewFun)
        }
    }

    fun <E> get(
        key: String, saveMode: SaveMode, encoder: Encoder<E>?,
        nullable: Boolean = false, renewFun: () -> E? = { null }
    ): E? {
        val cache = getCache<E>(key, saveMode)
        if (cache?.checkValid(config.defExpires) == true) {
            cache.read(encoder).let {
                if (it != null || nullable) return it
            }
        }
        return renewFun().apply {
            if (this != null || nullable) cache(key, saveMode, this, encoder)
        }
    }

    private fun <T> getCache(key: String, saveMode: SaveMode): AnyMemoryCache<T>? =
        try {
            cacheContainer.get(key) ?: kotlin.run {
                if (saveMode != SaveMode.MEMORY_ONLY)
                    cacheContainer.create(key, saveMode)
                else null
            }
        } catch (e: NoCacheError) {
            null
        }

    fun <T> cache(key: String, saveMode: SaveMode, value: T?, encoder: Encoder<T>?) {
        (cacheContainer.get<T>(key)?.apply {
            this.saveMode = saveMode
        } ?: cacheContainer.create(key, saveMode)).write(value, encoder)
    }

    companion object {
        class Cache(private val config: CacheConfig) {
            private val anyCacheMap: CoroutinesMutableMap<
                    String, AnyMemoryCache<*>> = coroutinesMutableMapOf(true)

            fun getKeys() = anyCacheMap.keys

            fun <T> getCache(key: String): AnyMemoryCache<T>? {
                return anyCacheMap[key]?.takeIf { cache ->
                    (cache.checkValid(config.defExpires)).also {
                        if (!it && config.autoRemove) remove(cache.key)
                    }
                }?.let {
                    it as AnyMemoryCache<T>
                }
            }

            fun <T> create(_key: String, saveMode: SaveMode): AnyMemoryCache<T> {
                val key = _key.md5()
                return AnyMemoryCache<T>(key, config, saveMode).apply {
                    anyCacheMap[key] = this
                }
            }

            fun <T> get(_key: String): AnyMemoryCache<T>? {
                return getCache(_key.md5())
            }

            private fun remove(key: String) {
                anyCacheMap.remove(key)?.delete()
            }
        }
    }
}