package net.xzos.upgradeall.core.oberver

private const val DEFAULT_TAG = "NULL"

abstract class Informer {
    private val observerMap: MutableMap<String, MutableList<Observer>> = mutableMapOf()

    fun notifyChanged(vararg vars: Any) {
        for (observerEntry in observerMap) {
            for (observer in observerEntry.value) {
                observer.onChanged(*vars)
            }
        }
    }

    fun notifyChanged(tag: String, vararg vars: Any) {
        for (observer in observerMap[tag] ?: return) {
            observer.onChanged(*vars)
        }
    }

    fun observeForever(observer: Observer) {
        observeForever(DEFAULT_TAG, observer)
    }

    fun observeForever(tag: String, observer: Observer) {
        (observerMap[tag] ?: mutableListOf<Observer>().apply {
            observerMap[tag] = this
        }).add(observer)
    }

    fun removeObserver(observer: Observer) {
        for (observerEntry in observerMap) {
            if (observerEntry.value.contains(observer)) observerEntry.value.remove(observer)
        }
    }

    fun removeObserver(tag: String) {
        observerMap.remove(tag)
    }
}