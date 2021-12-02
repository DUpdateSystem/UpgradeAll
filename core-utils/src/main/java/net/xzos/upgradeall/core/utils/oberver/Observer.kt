package net.xzos.upgradeall.core.utils.oberver

import net.xzos.upgradeall.core.utils.None

typealias ObserverFun<E> = (arg: E) -> Unit
typealias ObserverFunNoArg = () -> Unit

interface BaseObserverChecker {
    fun enableCheck(): Boolean
    fun removeCheck(): Boolean
    fun disableCheck(): Boolean
}

interface BaseObserver<E> : BaseObserverChecker {
    val id: Any
    fun call(arg: E)
}

open class Observer<E>(
    val function: ObserverFun<E>, override val id: Any = function
) : BaseObserver<E> {
    override fun call(arg: E) = function(arg)
    override fun enableCheck() = true
    override fun removeCheck() = false
    override fun disableCheck() = false
}

open class ObserverNoArg(
    function: ObserverFunNoArg, id: Any = function
) : Observer<None>(
    { function() }, id
)

abstract class ObserverFunWithChecker<E>(
    val function: ObserverFun<E>, override val id: Any = function
) : BaseObserver<E> {
    override fun call(arg: E) = function(arg)
}

abstract class ObserverFunWithCheckerNoArg(
    function: ObserverFunNoArg, id: Any = function
) : ObserverFunWithChecker<None>(
    { function() }, id
)