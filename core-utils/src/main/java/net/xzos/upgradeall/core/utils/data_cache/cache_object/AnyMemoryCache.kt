package net.xzos.upgradeall.core.utils.data_cache.cache_object

import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf

class AnyMemoryCache<T>(key: String) : BaseCache<T>(key) {

    @Suppress("UNCHECKED_CAST")
    override val store = cacheMap.getOrPut(key) {
        object : BaseStore<T> {

            private var time: Long = 0L
            private var data: T? = null

            override fun setTime(time: Long) {
                this.time = time
            }

            override fun getTime(): Long {
                return time
            }

            override fun read(): T {
                return data!!
            }

            override fun delete() {
                cacheMap.remove(key)
            }

            override fun write(data: T) {
                this.data = data
            }
        }
    } as BaseStore<T>

    fun write(any: T?) {
        write(any, PassEncoder())
    }

    fun read(): T? {
        return read(PassEncoder())
    }

    companion object {
        private val cacheMap = coroutinesMutableMapOf<String, BaseStore<*>>()
    }
}