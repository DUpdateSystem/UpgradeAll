package net.xzos.upgradeall.core.oberver

private const val DEFAULT_TAG = "NULL"

interface Informer {

    fun notifyChanged(vararg vars: Any) {
        notifyChanged(DEFAULT_TAG, *vars)
    }

    fun notifyChanged(tag: String, varObj: Any) {
        notifyChanged(tag, vars = *arrayOf(varObj))
    }

    fun notifyChanged(tag: String, vararg vars: Any) {
        val observerMap = observerMap.getObserverMap(this)
        for (observer in observerMap[tag] ?: return) {
            observer.onChanged(*vars)
        }
    }

    fun observeForever(observer: Observer) {
        observeForever(DEFAULT_TAG, observer)
    }

    fun observeForever(tag: String, observer: Observer) {
        val observerMap = observerMap.getObserverMap(this)
        val observerList = observerMap[tag] ?: mutableListOf<Observer>().apply {
            observerMap[tag] = this
        }
        if (!observerList.contains(observer))
            observerList.add(observer)
    }

    fun removeObserver(observer: Observer) {
        val observerMap = observerMap.getObserverMap(this)
        val observerKeyList = mutableListOf<String>()
        for (observerEntry in observerMap) {
            val observerKey = observerEntry.key
            val observerList = observerEntry.value
            if (observerList.contains(observer)) {
                observerList.remove(observer)
                if (observerList.isEmpty())
                    observerKeyList.add(observerKey)
            }
        }
        for (key in observerKeyList) {
            observerMap.remove(key)
        }
    }

    fun removeObserver(tag: String) {
        val observerMap = observerMap.getObserverMap(this)
        observerMap.remove(tag)
    }

    fun finalize() {
        observerMap.remove(this)
    }

    companion object {
        private val observerMap: MutableMap<Informer, MutableMap<String, MutableList<Observer>>> = mutableMapOf()

        private fun MutableMap<Informer, MutableMap<String, MutableList<Observer>>>.getObserverMap(informer: Informer)
                : MutableMap<String, MutableList<Observer>> {
            return this[informer]
                    ?: mutableMapOf<String, MutableList<Observer>>().also {
                        observerMap[informer] = it
                    }
        }
    }
}
