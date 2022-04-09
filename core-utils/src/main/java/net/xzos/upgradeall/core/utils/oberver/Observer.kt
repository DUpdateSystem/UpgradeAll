package net.xzos.upgradeall.core.utils.oberver

typealias Func<E> = (E) -> Unit
typealias FuncNoArg = () -> Unit

class Observer<E>(
    val func: Func<E>,
    val runCheck: () -> Boolean,
    val removeCheck: () -> Boolean,
    val id: Any = func,
)