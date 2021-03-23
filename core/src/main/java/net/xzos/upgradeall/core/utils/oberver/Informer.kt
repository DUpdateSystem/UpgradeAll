package net.xzos.upgradeall.core.utils.oberver

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
        runBlocking {
            getMutex(this@Informer).withLock {
                val observerMap = getObserverMap(this@Informer)
                for (observer in observerMap.getObserverMutableList<E>(tag, false)) {
                    observer(arg)
                }
            }
        }
    }

    fun <E> observeOneOff(observerFun: ObserverFun<E>) {
        observeOneOff(DEFAULT_TAG, observerFun)
    }

    fun <E> observeForever(observerFun: ObserverFun<E>) {
        observeForever(DEFAULT_TAG, observerFun)
    }

    fun <E> observeOneOff(tag: String, observerFun: ObserverFun<E>) {
        observeWithChecker(tag, observerFun, { true }, { true })
    }

    fun <E> observeWithChecker(
            tag: String, observerFun: ObserverFun<E>,
            checkerStartFun: () -> Boolean, checkerRemoveFun: () -> Boolean
    ) {
        val observeOneOffFun = fun(arg: E) {
            if (checkerStartFun())
                observerFun(arg)
            if (checkerRemoveFun())
                removeObserver(observerFun)
        }
        observeForever(tag, observeOneOffFun)
    }

    fun <E> observeForever(tag: String, observerFun: ObserverFun<E>) {
        runBlocking {
            getMutex(this@Informer).withLock {
                val observerMap = getObserverMap(this@Informer)
                val observerList = observerMap.getObserverMutableList<E>(tag)
                if (!observerList.contains(observerFun))
                    observerList.add(observerFun)
            }
        }
    }

    fun <E> removeObserver(observerFun: ObserverFun<E>) {
        GlobalScope.launch {
            getMutex(this@Informer).withLock {
                val observerMap = getObserverMap(this@Informer)
                val observerKeyList = mutableListOf<String>()
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
                // 清除空列表
                for (key in observerKeyList) {
                    observerMap.remove(key)
                }
            }
        }
    }

    fun removeObserver(tag: String) {
        GlobalScope.launch {
            getMutex(this@Informer).withLock {
                val observerMap = getObserverMap(this@Informer)
                observerMap.remove(tag)
            }
        }
    }

    fun finalize() {
        runBlocking {
            getMutex(this@Informer).withLock {
                observerMap.remove(this@Informer)
                mutexMap.remove(this@Informer)
            }
        }
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