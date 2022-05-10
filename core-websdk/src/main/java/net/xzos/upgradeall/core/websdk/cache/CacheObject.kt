package net.xzos.upgradeall.core.websdk.cache

import java.io.File
import java.time.Instant

class CacheObject(private val file: File) {
    val time = file.lastModified()

    private fun checkValid(dataCacheTimeSec: Int): Boolean {
        return (Instant.now().epochSecond - time <= dataCacheTimeSec)
    }

    fun readBytes() = file.readBytes()
}