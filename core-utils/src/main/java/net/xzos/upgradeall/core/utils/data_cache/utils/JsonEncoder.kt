package net.xzos.upgradeall.core.utils.data_cache.utils

import net.xzos.upgradeall.core.utils.data_cache.cache_object.Encoder
import org.json.JSONArray
import org.json.JSONObject
import kotlin.text.Charsets.UTF_8

object JsonObjectEncoder : Encoder<JSONObject> {
    override fun encode(value: JSONObject?): ByteArray {
        return value?.toString(0)?.toByteArray() ?: byteArrayOf()
    }

    override fun decode(bytes: ByteArray): JSONObject? {
        return try {
            JSONObject(bytes.toString(UTF_8))
        } catch (e: Throwable) {
            null
        }
    }
}

object JsonArrayEncoder : Encoder<JSONArray> {
    override fun encode(value: JSONArray?): ByteArray {
        return value?.toString(0)?.toByteArray() ?: byteArrayOf()
    }

    override fun decode(bytes: ByteArray): JSONArray? {
        return try {
            JSONArray(bytes.toString(UTF_8))
        } catch (e: Throwable) {
            null
        }
    }
}
