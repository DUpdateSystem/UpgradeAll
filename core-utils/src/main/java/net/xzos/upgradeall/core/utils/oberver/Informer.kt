package net.xzos.upgradeall.core.utils.oberver

import net.xzos.upgradeall.core.utils.coroutines.*
import net.xzos.upgradeall.core.utils.none

private val DEFAULT_TAG = object : Tag {}

interface Informer {

    val informerId: Int

    fun notifyChanged() {
        notifyChanged(DEFAULT_TAG)
    }

    fun notifyChanged(tag: Tag) {
        notifyChanged(tag, none)
    }

    fun <E> notifyChanged(arg: E) {
        notifyChanged(DEFAULT_TAG, arg)
    }

    fun <E> notifyChanged(tag: Tag, arg: E) {
        val observerMap = getObserverMap(informerId, false)
        for (observer in getExistObserverList<E>(tag, observerMap)) {
            if (observer.enableCheck())
                observer.call(arg)
            if (observer.removeCheck())
                removeObserver(tag, observer)
        }
    }

    fun <E> observeForever(observerFun: ObserverFun<E>) {
        observeForever(DEFAULT_TAG, observerFun)
    }

    fun <E> observeOneOff(observerFun: ObserverFun<E>) {
        observeOneOff(DEFAULT_TAG, observerFun)
    }

    fun <E> observeOneOff(tag: Tag, observerFun: ObserverFun<E>) {
        observeWithChecker(tag, observerFun, { true }, { true })
    }

    fun observeForever(observerFun: ObserverFunNoArg) {
        observeForever(DEFAULT_TAG, observerFun)
    }

    fun observeOneOff(observerFun: ObserverFunNoArg) {
        observeOneOff(DEFAULT_TAG, observerFun)
    }

    fun observeOneOff(tag: Tag, observerFun: ObserverFunNoArg) {
        observeWithChecker(tag, observerFun, { true }, { true })
    }

    fun <E> observeWithChecker(
        tag: Tag, observerFun: ObserverFun<E>,
        checkerStartFun: () -> Boolean, checkerRemoveFun: () -> Boolean
    ) {
        observeForever(tag, object : ObserverFunWithChecker<E>(observerFun) {
            override fun enableCheck(): Boolean = checkerStartFun()
            override fun removeCheck(): Boolean = checkerRemoveFun()
            override fun disableCheck(): Boolean = !checkerStartFun()
        })
    }

    fun observeWithChecker(
        tag: Tag, observerFun: ObserverFunNoArg,
        checkerStartFun: () -> Boolean, checkerRemoveFun: () -> Boolean
    ) {
        observeForever(tag, object : ObserverFunWithCheckerNoArg(observerFun) {
            override fun enableCheck(): Boolean = checkerStartFun()
            override fun removeCheck(): Boolean = checkerRemoveFun()
            override fun disableCheck(): Boolean = !checkerStartFun()
        })
    }

    fun <E> observeForever(tag: Tag, function: ObserverFun<E>) {
        observeForever(tag, Observer(function))
    }

    fun observeForever(tag: Tag, function: ObserverFunNoArg) {
        observeForever(tag, ObserverNoArg(function))
    }

    fun observeForever(tag: Tag, observer: BaseObserver<*>) {
        val observerMap = getObserverMap(informerId)
        val observerList = getObserverList(tag, observerMap)
        if (!observerList.containsId(observer))
            observerList.add(observer)
    }

    fun <E> removeObserver(observerFun: ObserverFun<E>) {
        getObserverMap(informerId, false).forEach {
            it.value.run {
                forEach { observer ->
                    if (observer.id == observerFun)
                        remove(observer)
                }
            }
        }
    }

    fun removeObserver(observerFun: ObserverFunNoArg) {
        getObserverMap(informerId, false).forEach {
            it.value.run {
                forEach { observer ->
                    if (observer.id == observerFun)
                        remove(observer)
                }
            }
        }
    }

    fun removeObserver(tag: Tag, observer: BaseObserver<*>) {
        getObserverMap(informerId, false)[tag]?.remove(observer)
    }

    fun removeObserver(tag: Tag) {
        getObserverMap(informerId, false).remove(tag)
    }

    fun finalize() {
        observerMap.remove(informerId)
    }

    companion object {
        private fun List<BaseObserver<*>>.containsId(value: Any): Boolean {
            val id = if (value is Observer<*>)
                value.id
            else value
            forEach { observer ->
                if (observer.id == id)
                    return true
            }
            return false
        }

        private val observerMap: CoroutinesMutableMap<Int, CoroutinesMutableMap<Tag, CoroutinesMutableList<BaseObserver<*>>>> =
            coroutinesMutableMapOf(true)
        private val informerIdGetter: CoroutinesCount = CoroutinesCount(0)

        fun getInformerId(): Int = informerIdGetter.getNewValue(1)

        private fun getObserverMap(
            informerId: Int,
            createNew: Boolean = true
        ): CoroutinesMutableMap<Tag, CoroutinesMutableList<BaseObserver<*>>> {
            return (if (createNew)
                observerMap.getOrDefault(informerId) { coroutinesMutableMapOf(true) }
            else
                observerMap[informerId] ?: coroutinesMutableMapOf(true))

        }

        fun getObserverList(
            tag: Tag,
            observerMap: CoroutinesMutableMap<Tag, CoroutinesMutableList<BaseObserver<*>>>,
        ): MutableList<BaseObserver<*>> {
            return observerMap.getOrDefault(tag) { coroutinesMutableListOf(true) }
        }

        fun <E> getExistObserverList(
            tag: Tag,
            observerMap: CoroutinesMutableMap<Tag, CoroutinesMutableList<BaseObserver<*>>>,
        ): List<BaseObserver<E>> {
            return (observerMap[tag] ?: emptyList()) as List<BaseObserver<E>>
        }
    }
}