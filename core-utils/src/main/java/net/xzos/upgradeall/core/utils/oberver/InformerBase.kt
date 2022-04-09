package net.xzos.upgradeall.core.utils.oberver

import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf

abstract class InformerBase<E : Any?> {

    fun observeAlways(
        observerFun: Func<E?>,
        checkerRunFun: () -> Boolean = { true },
        checkerRemoveFun: () -> Boolean = { false },
        id: Any = observerFun
    ) {
        map.getList(doTag).add(Observer(observerFun, checkerRunFun, checkerRemoveFun, id))
    }

    protected fun pNotifyChanged(tag: Tag, arg: E?) {
        map.filterKeys {
            it == tag || it == doTag
        }.forEach {
            it.value.forEach { observer ->
                if (observer.runCheck())
                    observer.func(arg)
                if (observer.removeCheck())
                    removeObserver(tag, observer)
            }
        }
    }

    protected fun pObserve(
        tag: Tag, observerFun: Func<E?>,
        checkerRunFun: () -> Boolean = { true },
        checkerRemoveFun: () -> Boolean = { false },
        id: Any
    ) {
        map.getList(tag).add(Observer(observerFun, checkerRunFun, checkerRemoveFun, id))
    }

    private fun removeObserver(tag: Tag, observer: Observer<E?>) {
        map[tag]?.run {
            remove(observer)
            checkObserverList(tag)
        }
    }

    private fun checkObserverList(tag: Tag) {
        if (map[tag]?.isEmpty() == true) {
            map.remove(tag)
        }
    }

    protected val map =
        coroutinesMutableMapOf<Tag, CoroutinesMutableList<Observer<E?>>>(true)

    private fun CoroutinesMutableMap<Tag, CoroutinesMutableList<Observer<E?>>>.getList(k: Tag) =
        this.getOrDefault(k) { coroutinesMutableListOf(true) }

    protected fun CoroutinesMutableList<Observer<E?>>.remove(func: Func<E>) {
        this.removeIf { it.func == func }
    }

    protected fun CoroutinesMutableList<Observer<E?>>.remove(func: FuncNoArg) {
        this.removeIf { it.func == func }
    }
}