package net.xzos.upgradeall.core.oberver

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DEFAULT_TAG = "NULL"

interface Informer {

    fun notifyChanged() {
        notifyChanged(DEFAULT_TAG, Unit)
    }

    fun notifyChanged(tag: String) {
        notifyChanged(tag, Unit)
    }

    fun <E> notifyChanged(arg: E) {
        notifyChanged(DEFAULT_TAG, arg)
    }

    fun <E> notifyChanged(tag: String, arg: E) {
        val observerMap = getObserverMap(this)
        runBlocking {
            getMutex(this@Informer).withLock {
                for (observer in observerMap.getObserverMutableList<E>(tag, false)) {
                    observer(arg)
                }
            }
        }
    }

    fun <E> observeForever(observerFun: ObserverFun<E>) {
        observeForever(DEFAULT_TAG, observerFun)
    }

    fun <E> observeForever(tag: String, observerFun: ObserverFun<E>) {
        val observerMap = getObserverMap(this)
        runBlocking {
            getMutex(this@Informer).withLock {
                val observerList = observerMap.getObserverMutableList<E>(tag)
                if (!observerList.contains(observerFun))
                    observerList.add(observerFun)
            }
        }
    }

    fun <E> removeObserver(observerFun: ObserverFun<E>) {
        val observerMap = getObserverMap(this)
        val observerKeyList = mutableListOf<String>()
        GlobalScope.launch {
            getMutex(this@Informer).withLock {
                for (observerEntry in observerMap) {
                    val observerKey = observerEntry.key

                    @Suppress("UNCHECKED_CAST")
                    val observerList = observerEntry.value as MutableList<ObserverFun<E>>
                    // 删除 observer
                    if (observerList.contains(observerFun))
                        observerList.remove(observerFun)
                    // 记录为空的列表
                    if (observerList.isEmpty())
                        observerKeyList.add(observerKey)
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
        private val observerMap: MutableMap<Informer, MutableMap<String, Any>> = mutableMapOf()
        private val mutexMap: MutableMap<Informer, Mutex> = mutableMapOf()

        private fun getObserverMap(informer: Informer)
                : MutableMap<String, Any> = observerMap[informer]
                ?: mutableMapOf<String, Any>().also {
                    observerMap[informer] = it
                }

        fun <E> MutableMap<String, Any>.getObserverMutableList(
                key: String, createNew: Boolean = true
        ): MutableList<ObserverFun<E>> {
            @Suppress("UNCHECKED_CAST")
            return if (this.containsKey(key))
                this[key]!! as MutableList<ObserverFun<E>>
            else mutableListOf<ObserverFun<E>>().also {
                if (createNew)
                    this[key] = it
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
