package net.xzos.upgradeall.utils.file

val String.fileName: String
    get() = this.substringAfterLast('/', "")
