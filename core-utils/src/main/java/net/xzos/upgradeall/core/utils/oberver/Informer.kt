package net.xzos.upgradeall.core.utils.oberver

abstract class InformerNoClass : InformerBase<Any>() {
    fun notifyChanged() {
        pNotifyChanged(doTag, null)
    }

    fun observe(
        observerFun: FuncNoArg,
        checkerRunFun: () -> Boolean = { true },
        checkerRemoveFun: () -> Boolean = { false },
    ) {
        pObserve(doTag, { observerFun() }, checkerRunFun, checkerRemoveFun, observerFun)
    }

    fun removeObserver(func: FuncNoArg) {
        map.forEach {
            it.value.remove(func)
        }
    }
}

abstract class InformerNoTag<E> : InformerBase<E>() {
    fun notifyChanged(arg: E) {
        pNotifyChanged(doTag, arg)
    }

    fun observe(
        observerFun: Func<E>,
    ) {
        pObserve(doTag, { it?.run { observerFun(this) } }, id = observerFun)
    }

    fun observe(
        observerFun: Func<E>,
        checkerRunFun: () -> Boolean = { true },
        checkerRemoveFun: () -> Boolean = { !checkerRunFun() },
    ) {
        pObserve(
            doTag, { it?.run { observerFun(this) } },
            checkerRunFun, checkerRemoveFun, observerFun
        )
    }

    fun removeObserver(func: Func<E>) {
        map.forEach {
            it.value.remove(func)
        }
    }
}

abstract class InformerNoArg<T : Tag> : InformerBase<Any>() {
    fun notifyChanged(tag: T) {
        pNotifyChanged(tag, null)
    }

    fun observe(
        tag: T, observerFun: FuncNoArg,
        checkerRunFun: () -> Boolean = { true },
        checkerRemoveFun: () -> Boolean = { false },
    ) {
        pObserve(
            tag, { it?.run { observerFun() } },
            checkerRunFun, checkerRemoveFun, observerFun
        )
    }

    fun observe(tag: T, observerFun: FuncNoArg) {
        pObserve(tag, { it?.run { observerFun() } }, id = observerFun)
    }

    fun removeObserver(tag: T) {
        map.remove(tag)
    }

    fun removeObserver(tag: T, func: FuncNoArg) {
        map[tag]?.remove(func)
    }

    fun removeObserver(func: FuncNoArg) {
        map.forEach {
            it.value.remove(func)
        }
    }
}

abstract class Informer<T : Tag, E : Any> : InformerBase<E>() {
    fun notifyChanged(tag: T, arg: E) {
        pNotifyChanged(tag, arg)
    }

    fun observe(
        tag: T, observerFun: Func<E>,
        checkerRunFun: () -> Boolean = { true },
        checkerRemoveFun: () -> Boolean = { false },
    ) {
        pObserve(
            tag, { it?.run { observerFun(this) } },
            checkerRunFun, checkerRemoveFun, observerFun
        )
    }

    fun observe(tag: T, observerFun: Func<E>) {
        pObserve(tag, { it?.run { observerFun(this) } }, id = observerFun)
    }

    fun removeObserver(tag: T) {
        map.remove(tag)
    }

    fun removeObserver(tag: T, func: Func<E>) {
        map[tag]?.remove(func)
    }

    fun removeObserver(func: Func<E>) {
        map.forEach {
            it.value.remove(func)
        }
    }
}

abstract class InformerNullable<T : Tag, E : Any?> : InformerBase<E>() {
    fun notifyChanged(tag: T, arg: E?) {
        pNotifyChanged(tag, arg)
    }

    fun observe(
        tag: T,
        checkerRunFun: () -> Boolean = { true },
        checkerRemoveFun: () -> Boolean = { false },
        observerFun: Func<E?>,
    ) {
        pObserve(
            tag, { it?.run { observerFun(this) } },
            checkerRunFun, checkerRemoveFun, observerFun
        )
    }

    fun removeObserver(tag: T) {
        map.remove(tag)
    }

    fun removeObserver(tag: T, func: Func<E?>) {
        map[tag]?.remove(func)
    }

    fun removeObserver(func: Func<E?>) {
        map.forEach {
            it.value.remove(func)
        }
    }
}
