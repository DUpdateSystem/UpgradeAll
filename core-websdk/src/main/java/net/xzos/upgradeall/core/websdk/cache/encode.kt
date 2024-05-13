package net.xzos.upgradeall.core.websdk.cache

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.utils.data_cache.cache_object.BytesEncoder
import kotlin.text.Charsets.UTF_8


private val gson = Gson()

object CloudConfigListEncoder : BytesEncoder<net.xzos.upgradeall.websdk.data.json.CloudConfigList> {
    override fun encode(obj: net.xzos.upgradeall.websdk.data.json.CloudConfigList?): ByteArray {
        if (obj == null) return byteArrayOf()
        return gson.toJson(obj).toByteArray()
    }

    override fun decode(data: ByteArray): net.xzos.upgradeall.websdk.data.json.CloudConfigList? {
        return gson.fromJson(data.toString(UTF_8), net.xzos.upgradeall.websdk.data.json.CloudConfigList::class.java)
    }
}

object AppReleaseListEncoder : BytesEncoder<List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>> {
    override fun encode(obj: List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>?): ByteArray {
        if (obj == null) return byteArrayOf()
        return gson.toJson(obj).toByteArray()
    }

    override fun decode(data: ByteArray): List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>? {
        return Gson().fromJson(
            data.toString(UTF_8),
            object : TypeToken<List<net.xzos.upgradeall.websdk.data.json.ReleaseGson>?>() {}.type
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