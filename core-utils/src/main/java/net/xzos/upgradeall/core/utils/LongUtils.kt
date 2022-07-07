package net.xzos.upgradeall.core.utils

fun Double.fmt(): String {
    return if (this == this.toLong().toDouble())
        String.format("%d", this.toLong())
    else String.format("%s", this)
}