package net.xzos.upgradeall.core.utils.data_cache

import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.data_cache.cache_object.AnyMemoryCache
import net.xzos.upgradeall.core.utils.data_cache.cache_object.Encoder
import net.xzos.upgradeall.core.utils.data_cache.cache_object.NoCacheError
import net.xzos.upgradeall.core.utils.md5


class DataCacheManager(
    var config: CacheConfig,
    private val autoRemove: Boolean = true,
) {

    private val cache = Cache()

    fun <T> getAll(): Map<String, T?> {
        val map = mutableMapOf<String, T?>()
        cache.anyCacheMap.forEach {
            if (it.value.checkValid(config.defExpires)) {
                if (autoRemove) remove(it.key)
            } else {
                map[it.key] = it.value.read() as T
            }
        }
        return map
    }

    fun <E> get(key: String, encoder: Encoder<E>): E? {
        return get(key, encoder) { null }
    }

    fun <E> get(
        key: String, encoder: Encoder<E>?,
        renewFun: () -> E?
    ): E? {
        val cache = getCache<E>(key)
        return if (cache?.checkValid(config.defExpires) != true)
            renewFun().apply { cache(key, this, encoder) }
        else cache.read(encoder)
    }

    private fun <T> getCache(key: String): AnyMemoryCache<T>? =
        getCache(key.md5(), cache.anyCacheMap, null)

    private fun <T> getCache(
        key: String,
        map: MutableMap<String, AnyMemoryCache<*>>,
        encoder: Encoder<T>?
    ): AnyMemoryCache<T>? = map[key]?.let { it as AnyMemoryCache<T> }
        ?: try {
            AnyMemoryCache<T>(key, config).apply {
                read(encoder)
                map[key] = this
            }
        } catch (e: NoCacheError) {
            null
        }


    fun <T> cache(_key: String, value: T?, encoder: Encoder<T>?) {
        val key = _key.md5()
        cache.getCache(key) { AnyMemoryCache<T>(key, config) }.write(value, encoder)
    }

    fun remove(key: String) {
        cache.anyCacheMap.remove(key)?.delete()
    }

    companion object {
        class Cache(
            val anyCacheMap: CoroutinesMutableMap<
                    String, AnyMemoryCache<*>> = coroutinesMutableMapOf(true),
        ) {
            fun <T> getCache(key: String, defValueFun: () -> AnyMemoryCache<T>) =
                anyCacheMap[key]?.let { it as AnyMemoryCache<T> }
                    ?: defValueFun().apply { anyCacheMap[key] = this }
        }
    }
}
