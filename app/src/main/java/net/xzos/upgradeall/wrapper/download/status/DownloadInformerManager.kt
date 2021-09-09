package net.xzos.upgradeall.wrapper.download.status

import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf

object DownloadInformerManager {
    private val map = coroutinesMutableMapOf<String, DownloadInformer>(true)

    fun get(id: String): DownloadInformer? = map[id]

    fun add(id: String, downloadStatus: DownloadInformer) {
        map[id]?.also {
            if (it == downloadStatus) return
            it.unregister()
        }
        map[id] = downloadStatus
    }

    fun remove(downloadStatus: DownloadInformer) {
        map.forEach {
            if (it.value == downloadStatus)
                map.remove(it.key)
        }
    }
}