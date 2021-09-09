package net.xzos.upgradeall.utils.file

val String.extension: String
    get() = this.substringAfterLast('.', "")
