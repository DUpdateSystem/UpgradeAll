package net.xzos.upgradeall.core.utils.data_cache.utils

import net.xzos.upgradeall.core.utils.data_cache.cache_object.BytesEncoder
import org.json.JSONArray
import org.json.JSONObject
import kotlin.text.Charsets.UTF_8

object JsonObjectEncoder : BytesEncoder<JSONObject> {
    override fun encode(obj: JSONObject?): ByteArray {
        return obj?.toString(0)?.toByteArray() ?: byteArrayOf()
    }

    override fun decode(data: ByteArray): JSONObject? {
        return try {
            JSONObject(data.toString(UTF_8))
        } catch (e: Throwable) {
            null
        }
    }
}

object JsonArrayEncoder : BytesEncoder<JSONArray> {
    override fun encode(obj: JSONArray?): ByteArray {
        return obj?.toString(0)?.toByteArray() ?: byteArrayOf()
    }

    override fun decode(data: ByteArray): JSONArray? {
        return try {
            JSONArray(data.toString(UTF_8))
        } catch (e: Throwable) {
            null
        }
    }
}
