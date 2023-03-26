package net.xzos.upgradeall.core.utils.data_cache.cache_object

import net.xzos.upgradeall.core.utils.data_cache.CacheConfig
import java.io.File

class BytesDiskCache(key: String, config: CacheConfig) : BaseCache<ByteArray>(key) {

    override val store = object : BaseStore<ByteArray> {
        private val file = File(config.dir, key)

        override fun setTime(time: Long) {
            file.setLastModified(time)
        }

        override fun getTime(): Long {
            return file.lastModified()
        }

        override fun read(): ByteArray {
            return file.readBytes()
        }

        override fun delete() {
            file.delete()
        }

        override fun write(data: ByteArray) {
            file.parentFile?.mkdirs()
            file.createNewFile()
            file.writeBytes(data)
        }
    }
}