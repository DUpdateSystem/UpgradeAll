package net.xzos.upgradeall.core.websdk.cache

import net.xzos.upgradeall.core.utils.data_cache.cache_object.Encoder
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

object CloudConfigListEncoder : Encoder<CloudConfigList> {
    override fun encode(value: CloudConfigList?): ByteArray {
        TODO("Not yet implemented")
    }

    override fun decode(bytes: ByteArray): CloudConfigList? {
        TODO("Not yet implemented")
    }

}

object AppReleaseListEncoder : Encoder<List<ReleaseGson>> {
    override fun encode(value: List<ReleaseGson>?): ByteArray {
        TODO("Not yet implemented")
    }

    override fun decode(bytes: ByteArray): List<ReleaseGson>? {
        TODO("Not yet implemented")
    }

}
