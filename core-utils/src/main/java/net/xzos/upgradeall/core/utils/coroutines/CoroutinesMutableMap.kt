package net.xzos.upgradeall.core.utils.coroutines

import kotlinx.coroutines.sync.Mutex

class CoroutinesMutableMap<K, V>(hash: Boolean = false, map: Map<K, V>? = null) : MutableMap<K, V> {
    private val mutex = Mutex()
    private val mutableMap = if (hash) hashMapOf<K, V>() else mutableMapOf()

    init {
        map?.let { putAll(it) }
    }

    override val size: Int get() = mutableMap.size

    override fun containsKey(key: K): Boolean = mutableMap.containsKey(key)

    override fun containsValue(value: V): Boolean = mutableMap.containsValue(value)

    override fun get(key: K): V? {
        return mutex.runWithLock {
            mutableMap[key]
        }
    }

    fun getOrDefault(key: K, mkDefValue: () -> V): V {
        return mutex.runWithLock {
            mutableMap[key] ?: mkDefValue().apply {
                mutableMap[key] = this
            }
        }
    }

    override fun isEmpty(): Boolean = mutableMap.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = mutex.runWithLock { mutableMap.entries.toMutableSet() }
    override val keys: MutableSet<K>
        get() = mutex.runWithLock { mutableMap.keys.toMutableSet() }
    override val values: MutableCollection<V>
        get() = mutex.runWithLock { mutableMap.values.toMutableSet() }

    override fun clear() {
        mutex.runWithLock {
            mutableMap.clear()
        }
    }

    override fun put(key: K, value: V): V? {
        return mutex.runWithLock {
            mutableMap.put(key, value)
        }
    }

    override fun putAll(from: Map<out K, V>) {
        mutex.runWithLock {
            mutableMap.putAll(from)
        }
    }

    override fun remove(key: K): V? {
        return mutex.runWithLock {
            mutableMap.remove(key)
        }
    }
}

fun <K, V> coroutinesMutableMapOf(hash: Boolean = false) = CoroutinesMutableMap<K, V>(hash)
fun <E, V> Map<E, V>.toCoroutinesMutableMap(hash: Boolean = false) =
    CoroutinesMutableMap(hash, this)
