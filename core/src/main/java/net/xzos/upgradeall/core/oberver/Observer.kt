package net.xzos.upgradeall.core.oberver

interface Observer {
    fun onChanged(vararg vars: Any): Any?
}
