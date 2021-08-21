package net.xzos.upgradeall.core.utils.oberver

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

fun <T> Informer.observeWithLifecycleOwner(tag: String, owner: LifecycleOwner, observerFun: ObserverFun<T>) {
    val checker = fun(): Boolean = owner.lifecycle.currentState != Lifecycle.State.DESTROYED
    observeWithChecker(tag, observerFun, checker, { !checker() })
}

fun Informer.observeWithLifecycleOwner(tag: String, owner: LifecycleOwner, observerFun: ObserverFunNoArg) {
    val checker = fun(): Boolean = owner.lifecycle.currentState != Lifecycle.State.DESTROYED
    observeWithChecker(tag, observerFun, checker, { !checker() })
}