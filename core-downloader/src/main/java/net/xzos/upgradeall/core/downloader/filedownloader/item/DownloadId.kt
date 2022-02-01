package net.xzos.upgradeall.core.downloader.filedownloader.item


class DownloadId internal constructor(internal val isGroup: Boolean, internal val id: Int) {
    fun getKeyString() = "${if (isGroup) 'g' else ""}$id"
    override fun equals(other: Any?): Boolean {
        return other is DownloadId && other.isGroup == isGroup && other.id == id
    }

    override fun hashCode(): Int {
        var result = isGroup.hashCode()
        result = 31 * result + id
        return result
    }

    companion object {
        fun parsingIdString(s: String): DownloadId {
            val isGroup = s.first() == 'g'
            val id = s.replace("g", "").toInt()
            return DownloadId(isGroup, id)
        }
    }
}