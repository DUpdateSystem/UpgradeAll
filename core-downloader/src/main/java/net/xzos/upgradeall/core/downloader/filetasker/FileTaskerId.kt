package net.xzos.upgradeall.core.downloader.filetasker

class FileTaskerId(
    val name: String,
    val owner: Any? = null
) {
    override fun toString(): String {
        return name + owner.hashCode()
    }
}