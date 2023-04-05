package net.xzos.upgradeall.core.websdk.cache

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.utils.data_cache.cache_object.BytesEncoder
import net.xzos.upgradeall.core.websdk.json.CloudConfigList
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import kotlin.text.Charsets.UTF_8


private val gson = Gson()

object CloudConfigListEncoder : BytesEncoder<CloudConfigList> {
    override fun encode(obj: CloudConfigList?): ByteArray {
        if (obj == null) return byteArrayOf()
        return gson.toJson(obj).toByteArray()
    }

    override fun decode(data: ByteArray): CloudConfigList? {
        return gson.fromJson(data.toString(UTF_8), CloudConfigList::class.java)
    }
}

object AppReleaseListEncoder : BytesEncoder<List<ReleaseGson>> {
    override fun encode(obj: List<ReleaseGson>?): ByteArray {
        if (obj == null) return byteArrayOf()
        return gson.toJson(obj).toByteArray()
    }

    override fun decode(data: ByteArray): List<ReleaseGson>? {
        return Gson().fromJson(
            data.toString(UTF_8),
            object : TypeToken<List<ReleaseGson>?>() {}.type
        )
    }
}

object BoolEncoder : BytesEncoder<Boolean> {
    override fun encode(obj: Boolean?): ByteArray {
        if (obj == null) return byteArrayOf()
        return obj.toString().toByteArray()
    }

    override fun decode(data: ByteArray): Boolean {
        return data.toString().toBoolean()
    }
}