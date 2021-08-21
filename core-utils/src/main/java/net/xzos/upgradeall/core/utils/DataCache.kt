package net.xzos.upgradeall.core.utils

import android.os.Build
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import java.time.Instant


class DataCache(private val defExpires: Int) {

    private val time
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Instant.now().epochSecond
        else System.currentTimeMillis() / 1000

    private val cache = Cache()

    private fun Pair<Any?, Long>.expired(): Boolean {
        return time > this.second
    }

    fun getAll():Map<String, Any?>{
        val map = mutableMapOf<String, Any?>()
        cache.anyCacheMap.forEach {
            if (it.value.expired()) {
                remove(it.key)
            } else {
                map[it.key] = it.value.first
            }
        }
        return map
    }

    fun <E> get(key: String): E? {
        cache.anyCacheMap[key]?.also {
            if (it.expired()) {
                remove(key)
            } else {
                @Suppress("UNCHECKED_CAST")
                return it.first as E
            }
        }
        return null
    }

    fun cache(key: String, value: Any?, expiresSec: Int = defExpires) {
        cache.anyCacheMap[key] = Pair(value, time + expiresSec)
    }

    fun remove(key: String) {
        cache.anyCacheMap.remove(key)
    }

    companion object {
        class Cache(
            val anyCacheMap: CoroutinesMutableMap<String,
                    Pair<Any?, Long>> = coroutinesMutableMapOf(true),
        )
    }
}