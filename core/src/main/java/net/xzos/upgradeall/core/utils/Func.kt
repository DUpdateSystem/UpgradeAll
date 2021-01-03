package net.xzos.upgradeall.core.utils

/**
 * Callback interface used by Fetch to return
 * results to the caller.
 */
fun interface FuncR<E> {
    /**
     * Method called by Fetch to return requested information back to the caller.
     *
     * @param result Result of a request made by a caller. Result is never null.
     */
    fun call(result: E)
}

fun interface Func {
    fun call()
}