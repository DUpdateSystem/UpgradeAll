package net.xzos.upgradeall.core.module.network

import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.data.json.ReleaseGson
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import java.util.*


internal object DataCache {

    private val cache = Cache()

    private val dataExpirationTime get() = coreConfig.data_expiration_time

    private fun Pair<Any?, Calendar>.isExpired(): Boolean {
        val time = this.second
        time.add(Calendar.MINUTE, dataExpirationTime)
        return Calendar.getInstance().after(time)
    }

    fun <E> getAnyCache(key: String): E? {
        cache.anyCacheMap[key]?.also {
            if (!it.isExpired()) {
                @Suppress("UNCHECKED_CAST")
                return it.first as E
            } else cache.anyCacheMap.remove(key)
        }
        return null
    }

    fun cacheAny(key: String, value: Any) {
        cache.anyCacheMap[key] = Pair(value, Calendar.getInstance())
    }

    fun removeAnyCache(key: String) {
        cache.anyCacheMap.remove(key)
    }

    fun getAppRelease(
        hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>
    ): List<ReleaseGson>? {
        val key = hubUuid + auth + appId
        cache.appReleaseMap[key]?.also {
            if (!it.isExpired()) {
                return it.first
            } else cache.appReleaseMap.remove(key)
        }
        return null
    }

    fun cacheAppStatus(
        hubUuid: String, auth: Map<String, String?>, appId: Map<String, String?>,
        releaseList: List<ReleaseGson>?
    ) {
        val key = hubUuid + auth + appId
        if (cache.appReleaseMap[key]?.first != releaseList)
            cache.appReleaseMap[key] = Pair(releaseList, Calendar.getInstance())
    }

    data class Cache(
        internal val anyCacheMap: CoroutinesMutableMap<String,
                Pair<Any, Calendar>> = coroutinesMutableMapOf(true),
        internal val appReleaseMap: CoroutinesMutableMap<String,
                Pair<List<ReleaseGson>?, Calendar>> = coroutinesMutableMapOf(true)
    )
}