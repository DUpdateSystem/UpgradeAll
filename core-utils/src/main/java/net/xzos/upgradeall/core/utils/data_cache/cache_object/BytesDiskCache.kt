package net.xzos.upgradeall.core.utils.data_cache.cache_object

import net.xzos.upgradeall.core.utils.data_cache.CacheConfig
import java.io.File
import java.io.FileNotFoundException

class BytesDiskCache(key: String, config: CacheConfig) : BaseCache<ByteArray>(key) {
    private val file = File(config.dir, key)

    override var time: Long
        get() = file.lastModified() / 1000
        set(_) {}

    override fun write(any: ByteArray?) {
        any?.run {
            file.createNewFile()
            file.writeBytes(this)
        }
    }

    override fun read() = try {
        file.readBytes()
    } catch (e: FileNotFoundException) {
        null
    }

    override fun delete() = file.delete()
}