package net.xzos.upgradeall.core.websdk.cache

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.utils.data_cache.cache_object.Encoder
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.ReleaseGson


private val gson = Gson()

object CloudConfigListEncoder : Encoder<CloudConfigList> {
    override fun encode(value: CloudConfigList?): ByteArray {
        return gson.toJson(value).toByteArray()
    }

    override fun decode(bytes: ByteArray): CloudConfigList? {
        return gson.fromJson(bytes.toString(), CloudConfigList::class.java)
    }
}

object AppReleaseListEncoder : Encoder<List<ReleaseGson>> {
    override fun encode(value: List<ReleaseGson>?): ByteArray {
        return gson.toJson(value).toByteArray()
    }

    override fun decode(bytes: ByteArray): List<ReleaseGson>? {
        return Gson().fromJson(bytes.toString(), object : TypeToken<List<ReleaseGson>?>() {}.type)
    }
}