package net.xzos.upgradeall.core.utils.oberver

import net.xzos.upgradeall.core.utils.coroutines.*

private const val DEFAULT_TAG = "N_TAG"

interface Informer {

    val informerId: Int

    fun notifyChanged() {
        notifyChanged(DEFAULT_TAG)
    }

    fun notifyChanged(tag: String) {
        val observerMap = getObserverMap(informerId, false)
        for (observer in observerMap.getObserverMutableList<ObserverFunNoArg>(tag, false)) {
            observer()
        }
    }

    fun <E> notifyChanged(arg: E) {
        notifyChanged(DEFAULT_TAG, arg)
    }

    fun <E> notifyChanged(tag: String, arg: E) {
        val observerMap = getObserverMap(informerId, false)
        for (observer in observerMap.getObserverMutableList<ObserverFun<E>>(tag, false)) {
            observer(arg)
        }
    }

    fun <E> observeForever(observerFun: ObserverFun<E>) {
        observeForever(DEFAULT_TAG, observerFun)
    }

    fun <E> observeOneOff(observerFun: ObserverFun<E>) {
        observeOneOff(DEFAULT_TAG, observerFun)
    }

    fun <E> observeOneOff(tag: String, observerFun: ObserverFun<E>) {
        observeWithChecker(tag, observerFun, { true }, { true })
    }

    fun observeForever(observerFun: ObserverFunNoArg) {
        observeForever(DEFAULT_TAG, observerFun)
    }

    fun observeOneOff(observerFun: ObserverFunNoArg) {
        observeOneOff(DEFAULT_TAG, observerFun)
    }

    fun observeOneOff(tag: String, observerFun: ObserverFunNoArg) {
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

    fun observeWithChecker(
            tag: String, observerFun: ObserverFunNoArg,
            checkerStartFun: () -> Boolean, checkerRemoveFun: () -> Boolean
    ) {
        val observeOneOffFun = fun() {
            if (checkerStartFun())
                observerFun()
            if (checkerRemoveFun())
                removeObserver(observerFun)
        }
        observeForever(tag, observeOneOffFun)
    }

    fun <E> observeForever(tag: String, observerFun: ObserverFun<E>) {
        val observerMap = getObserverMap(informerId)
        val observerList = observerMap.getObserverMutableList<ObserverFun<E>>(tag)
        if (!observerList.contains(observerFun))
            observerList.add(observerFun)
    }

    fun observeForever(tag: String, observerFun: ObserverFunNoArg) {
        val observerMap = getObserverMap(informerId)
        val observerList = observerMap.getObserverMutableList<ObserverFunNoArg>(tag)
        if (!observerList.contains(observerFun))
            observerList.add(observerFun)
    }

    fun <E> removeObserver(observerFun: ObserverFun<E>) {
        getObserverMap(informerId, false).forEach {
            it.value.remove(observerFun)
        }
    }

    fun removeObserver(observerFun: ObserverFunNoArg) {
        getObserverMap(informerId, false).forEach {
            it.value.remove(observerFun)
        }
    }

    fun removeObserver(tag: String) {
        getObserverMap(informerId, false).remove(tag)
    }

    fun finalize() {
        observerMap.remove(informerId)
    }

    companion object {
        private val observerMap: CoroutinesMutableMap<Int, CoroutinesMutableMap<String, CoroutinesMutableList<Any>>> = coroutinesMutableMapOf(true)
        private val informerIdGetter: CoroutinesCount = CoroutinesCount(0)

        fun getInformerId(): Int = informerIdGetter.getNewValue(1)

        private fun getObserverMap(informerId: Int, createNew: Boolean = true): CoroutinesMutableMap<String, CoroutinesMutableList<Any>> = observerMap[informerId]
                ?: coroutinesMutableMapOf<String, CoroutinesMutableList<Any>>(true).also {
                    if (createNew) observerMap[informerId] = it

                }

        fun <E> CoroutinesMutableMap<String, CoroutinesMutableList<Any>>.getObserverMutableList(
                key: String, createNew: Boolean = true
        ): CoroutinesMutableList<E> {
            @Suppress("UNCHECKED_CAST")
            return if (this.containsKey(key))
                this[key]!! as CoroutinesMutableList<E>
            else coroutinesMutableListOf<E>().also {
                if (createNew) this[key] = it as CoroutinesMutableList<Any>
            }
        }
    }
}