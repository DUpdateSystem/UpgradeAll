package net.xzos.upgradeall.core.utils.data_cache.utils

import net.xzos.upgradeall.core.utils.data_cache.cache_object.Encoder
import kotlin.text.Charsets.UTF_8

object BytesEncoder : Encoder<ByteArray> {
    override fun encode(value: ByteArray?) = value ?: byteArrayOf()

    override fun decode(bytes: ByteArray) = bytes
}

object StringEncoder : Encoder<String> {
    override fun encode(value: String?) = value?.toByteArray() ?: byteArrayOf()

    override fun decode(bytes: ByteArray) = bytes.toString(UTF_8)
}
