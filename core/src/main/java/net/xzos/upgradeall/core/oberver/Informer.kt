package net.xzos.upgradeall.core.oberver

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DEFAULT_TAG = "NULL"

interface Informer {

    fun notifyChanged(vararg vars: Any) {
        notifyChanged(DEFAULT_TAG, *vars)
    }

    fun notifyChanged(tag: String, varObj: Any) {
        notifyChanged(tag, vars = *arrayOf(varObj))
    }

    fun notifyChanged(tag: String, vararg vars: Any) {
        val observerMap = getObserverMap(this)
        runBlocking {
            getMutex(this@Informer).withLock {
                for (observer in observerMap[tag] ?: return@runBlocking) {
                    observer.onChanged(*vars)
                }
            }
        }
    }

    fun observeForever(observer: Observer) {
        observeForever(DEFAULT_TAG, observer)
    }

    fun observeForever(tag: String, observer: Observer) {
        val observerMap = getObserverMap(this)
        runBlocking {
            getMutex(this@Informer).withLock {
                val observerList = observerMap[tag] ?: mutableListOf<Observer>().apply {
                    observerMap[tag] = this
                }
                if (!observerList.contains(observer))
                    observerList.add(observer)
            }
        }
    }

    fun removeObserver(observer: Observer) {
        val observerMap = getObserverMap(this)
        val observerKeyList = mutableListOf<String>()
        GlobalScope.launch {
            getMutex(this@Informer).withLock {
                for (observerEntry in observerMap) {
                    val observerKey = observerEntry.key
                    val observerList = observerEntry.value
                    // 删除 observer
                    if (observerList.contains(observer)) {
                        observerList.remove(observer)
                        // 记录为空的列表
                        if (observerList.isEmpty())
                            observerKeyList.add(observerKey)
                    }
                }
            }
        }
        // 清除空列表
        for (key in observerKeyList) {
            observerMap.remove(key)
        }
    }

    fun removeObserver(tag: String) {
        val observerMap = getObserverMap(this)
        observerMap.remove(tag)
    }

    fun finalize() {
        observerMap.remove(this)
        mutexMap.remove(this)
    }

    companion object {
        private val observerMap: MutableMap<Informer, MutableMap<String, MutableList<Observer>>> = mutableMapOf()
        private val mutexMap: MutableMap<Informer, Mutex> = mutableMapOf()

        private fun getObserverMap(informer: Informer)
                : MutableMap<String, MutableList<Observer>> {
            return observerMap[informer]
                    ?: mutableMapOf<String, MutableList<Observer>>().also {
                        observerMap[informer] = it
                    }
        }

        private fun getMutex(informer: Informer)
                : Mutex {
            return mutexMap[informer] ?: Mutex().also {
                mutexMap[informer] = it
            }
        }
    }
}
