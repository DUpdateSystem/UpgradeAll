package net.xzos.upgradeall.core.websdk.api.client_proxy

import net.xzos.upgradeall.core.utils.data_cache.cache_object.BytesEncoder

object StringEncoder : BytesEncoder<String> {
    override fun encode(obj: String?) = obj?.toByteArray()!!
    override fun decode(data: ByteArray) = data.toString()
}